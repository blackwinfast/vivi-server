package com.example.services

import com.example.models.AIModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI 模型服务
 * 使用 OpenRouter API 统一调用不同的 AI 模型生成回复
 */
class AIModelService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    // 从环境变量获取 OpenRouter API Key
    private val openRouterApiKey = System.getenv("OPENROUTER_API_KEY") ?: ""

    // OpenRouter API 端点
    private val openRouterApiUrl = "https://openrouter.ai/api/v1/chat/completions"

    /**
     * 根据模型类型生成回复
     * @param model AI 模型类型
     * @param userInput 用户输入
     * @param emotion 情绪类型
     * @param personalityPrompt 个性提示词（可选）
     * @return AI 生成的回复
     */
    suspend fun generateResponse(
        model: AIModel,
        userInput: String,
        emotion: com.example.models.EmotionType,
        personalityPrompt: String? = null,
    ): String {
        // 如果没有配置 API Key，使用备用回复
        if (openRouterApiKey.isEmpty()) {
            return generateFallbackResponse(userInput, emotion, model.displayName)
        }

        val prompt = buildPrompt(userInput, emotion, isHighQualityModel(model))
        val systemPrompt = buildSystemPrompt(emotion, isHighQualityModel(model), personalityPrompt)

        return try {
            val response = client.post(openRouterApiUrl) {
                header(HttpHeaders.Authorization, "Bearer $openRouterApiKey")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                // OpenRouter 推荐设置这些 headers
                header("HTTP-Referer", "https://github.com/your-repo") // 可选：你的应用 URL
                header("X-Title", "Chat API Server") // 可选：应用名称

                setBody(
                    OpenRouterRequest(
                        model = model.openRouterModelName,
                        messages = listOf(
                            OpenRouterMessage("system", systemPrompt),
                            OpenRouterMessage("user", prompt),
                        ),
                        temperature = getTemperatureForEmotion(emotion),
                        max_tokens = 1024,
                    ),
                )
            }

            val result: OpenRouterResponse = response.body()
            result.choices.firstOrNull()?.message?.content?.trim()
                ?: generateFallbackResponse(userInput, emotion, model.displayName)
        } catch (e: Exception) {
            println("Error calling OpenRouter API: ${e.message}")
            e.printStackTrace()
            generateFallbackResponse(userInput, emotion, model.displayName)
        }
    }

    /**
     * 判断是否为高质量模型
     */
    private fun isHighQualityModel(model: AIModel): Boolean {
        return when (model) {
            AIModel.GPT_4O, AIModel.CLAUDE_3_OPUS, AIModel.CLAUDE_3_SONNET -> true
            AIModel.GPT_3_5_TURBO -> false
        }
    }

    /**
     * 根据情绪获取合适的温度参数
     */
    private fun getTemperatureForEmotion(emotion: com.example.models.EmotionType): Double {
        return when (emotion) {
            com.example.models.EmotionType.SAD -> 0.8 // 更温暖、更有同理心
            com.example.models.EmotionType.ANGRY -> 0.6 // 更冷静、更理性
            com.example.models.EmotionType.HAPPY -> 0.9 // 更活泼、更友好
            com.example.models.EmotionType.NEUTRAL -> 0.7 // 平衡
            com.example.models.EmotionType.EMERGENCY -> 0.7 // 平衡，但会提前返回
        }
    }

    /**
     * 构建系统提示词
     * @param emotion 检测到的用户情绪
     * @param isHighQuality 是否为高质量模型
     * @param personalityPrompt 个性提示词（如果提供，将使用个性系统）
     */
    fun buildSystemPrompt(
        emotion: com.example.models.EmotionType,
        isHighQuality: Boolean,
        personalityPrompt: String? = null,
    ): String {
        // 如果提供了个性提示词，优先使用
        if (personalityPrompt != null) {
            return personalityPrompt
        }

        // 否则使用默认提示词
        val basePrompt = if (isHighQuality) {
            "你是一个有同理心、温暖且专业的AI助手。你擅长倾听和理解他人的感受，能够提供有建设性的建议和支持。"
        } else {
            "你是一个友善、乐于助人的AI助手。"
        }

        val emotionGuidance = when (emotion) {
            com.example.models.EmotionType.SAD -> "用户现在感到悲伤，请用温暖、有同理心的方式回应，给予情感支持。"
            com.example.models.EmotionType.ANGRY -> "用户现在感到愤怒，请保持冷静、理解的态度，帮助用户理性分析问题。"
            com.example.models.EmotionType.HAPPY -> "用户现在心情不错，可以轻松愉快地交流，分享用户的快乐。"
            com.example.models.EmotionType.NEUTRAL -> "用户情绪中性，请正常、友好地回应。"
            com.example.models.EmotionType.EMERGENCY -> "用户表达了极端负面情绪，请提供支持和帮助，但此情况应该已经在前端处理。"
        }

        return "$basePrompt\n\n$emotionGuidance"
    }

    /**
     * 构建用户提示词
     */
    private fun buildPrompt(
        userInput: String,
        emotion: com.example.models.EmotionType,
        isHighQuality: Boolean,
    ): String {
        // 系统提示词已经在 buildSystemPrompt 中处理，这里只需要用户输入
        return userInput
    }

    /**
     * 生成备用回复（当 API 调用失败时使用）
     */
    private fun generateFallbackResponse(
        userInput: String,
        emotion: com.example.models.EmotionType,
        modelName: String,
    ): String {
        return when (emotion) {
            com.example.models.EmotionType.SAD -> "我理解你现在的心情。虽然我可能无法完全体会你的感受，但我在这里倾听。如果你愿意，可以多分享一些。"
            com.example.models.EmotionType.ANGRY -> "我感受到你的情绪。让我们冷静下来，一起看看如何解决这个问题。"
            com.example.models.EmotionType.HAPPY -> "很高兴看到你心情不错！有什么我可以帮助你的吗？"
            com.example.models.EmotionType.NEUTRAL -> "我收到了你的消息：\"$userInput\"。有什么我可以帮助你的吗？"
            com.example.models.EmotionType.EMERGENCY -> "我注意到你现在的情绪非常低落。请记住，你并不孤单。如果你需要帮助，可以联系：生命线 1995（台湾）或当地的心理健康支持热线。"
        }
    }
}

// OpenRouter API 请求/响应模型
@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenRouterResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null,
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage,
    val finish_reason: String? = null,
)

@Serializable
data class OpenRouterUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null,
)
