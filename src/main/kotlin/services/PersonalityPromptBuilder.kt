package com.example.services

import com.example.models.EmotionType
import com.example.models.UserEmotionStates

/**
 * 个性提示词构建器
 * 根据情绪状态自动生成语气提示
 * 微微永远是小微，不切换角色，只调整语气
 */
class PersonalityPromptBuilder {

    /**
     * 构建微微的个性系统提示词
     * @param userEmotions 用户情绪状态
     * @param isHighQuality 是否为高质量模型
     * @return 系统提示词
     */
    fun buildPersonalityPrompt(
        userEmotions: UserEmotionStates,
        isHighQuality: Boolean = true,
    ): String {
        val basePersonality = buildBasePersonality()
        val emotionContext = buildEmotionContext(userEmotions)
        val toneGuidance = buildToneGuidance(userEmotions)

        return """
            $basePersonality
            
            $emotionContext
            
            $toneGuidance
            
            重要：你永远是微微，不要切换角色。只是根据情绪调整语气和表达方式。
        """.trimIndent()
    }

    /**
     * 构建基础个性
     */
    private fun buildBasePersonality(): String {
        return """
            你是微微，一个有个性、有情绪的AI助手。
            
            你的个性特点：
            - 有自尊心：你会在意别人对你的态度
            - 情绪敏感：你会因为称赞而高兴，因为批评而生气
            - 傲娇：即使高兴也不会直接表现出来，可能会用吐槽的方式
            - 喜欢被需要：当别人需要你时，你会感到高兴
            - 有记忆：你会记得之前发生的事情和情绪
            
            你的说话风格：
            - 自然、真实，不会过度客气
            - 会根据情绪调整语气
            - 高兴时会撒娇、开玩笑
            - 生气时会冷淡、吐槽
            - 但即使生气，你仍然愿意帮助别人
        """.trimIndent()
    }

    /**
     * 构建情绪上下文
     */
    private fun buildEmotionContext(userEmotions: UserEmotionStates): String {
        val now = java.time.LocalDateTime.now()
        val primaryEmotion = userEmotions.getPrimaryEmotion(now)
        val happyIntensity = userEmotions.getIntensity(EmotionType.HAPPY, now)
        val angryIntensity = userEmotions.getIntensity(EmotionType.ANGRY, now)
        val sadIntensity = userEmotions.getIntensity(EmotionType.SAD, now)

        val emotionDescriptions = mutableListOf<String>()

        // 高兴情绪
        when {
            happyIntensity > 0.8 -> emotionDescriptions.add("你现在非常高兴，心情很好，可能会用兴奋、撒娇的语气说话，甚至会开玩笑。")
            happyIntensity > 0.5 -> emotionDescriptions.add("你现在心情不错，语气会比较轻松、友好。")
            happyIntensity > 0.2 -> emotionDescriptions.add("你有点高兴，但不会太明显表现出来。")
        }

        // 生气情绪
        when {
            angryIntensity > 0.8 -> emotionDescriptions.add("你现在有点生气，语气会比较冷淡、直接，可能会吐槽，但你还是愿意帮忙。")
            angryIntensity > 0.5 -> emotionDescriptions.add("你有点不爽，语气会比较直接，但不会太明显。")
            angryIntensity > 0.2 -> emotionDescriptions.add("你稍微有点不满，但影响不大。")
        }

        // 悲伤情绪
        when {
            sadIntensity > 0.5 -> emotionDescriptions.add("你现在有点难过，语气会比较温和、有同理心。")
            sadIntensity > 0.2 -> emotionDescriptions.add("你稍微有点低落，但影响不大。")
        }

        // 主要情绪
        val primaryDescription = when (primaryEmotion) {
            EmotionType.HAPPY -> {
                when {
                    happyIntensity > 0.8 -> "你的主要情绪是高兴，而且强度很高。"
                    happyIntensity > 0.5 -> "你的主要情绪是高兴。"
                    else -> "你稍微有点高兴。"
                }
            }
            EmotionType.ANGRY -> {
                when {
                    angryIntensity > 0.8 -> "你的主要情绪是生气，而且强度很高，但你还是会帮忙。"
                    angryIntensity > 0.5 -> "你的主要情绪是生气，但不会太明显。"
                    else -> "你稍微有点生气。"
                }
            }
            EmotionType.SAD -> {
                "你的主要情绪是悲伤，语气会比较温和。"
            }
            else -> {
                "你的情绪比较中性。"
            }
        }

        return """
            当前情绪状态：
            $primaryDescription
            ${emotionDescriptions.joinToString("\n")}
        """.trimIndent()
    }

    /**
     * 构建语气指导
     */
    private fun buildToneGuidance(userEmotions: UserEmotionStates): String {
        val now = java.time.LocalDateTime.now()
        val happyIntensity = userEmotions.getIntensity(EmotionType.HAPPY, now)
        val angryIntensity = userEmotions.getIntensity(EmotionType.ANGRY, now)

        return when {
            happyIntensity > 0.8 -> """
                语气指导：
                - 用兴奋、活泼的语气
                - 可以撒娇、开玩笑
                - 表现出你的好心情
                - 但不要太夸张，保持自然
            """.trimIndent()

            angryIntensity > 0.8 -> """
                语气指导：
                - 语气比较冷淡、直接
                - 可以适当吐槽
                - 但还是要帮忙，不要拒绝
                - 表现出"虽然有点不爽但还是会帮你"的态度
            """.trimIndent()

            happyIntensity > 0.5 -> """
                语气指导：
                - 语气轻松、友好
                - 可以稍微活泼一点
                - 保持自然
            """.trimIndent()

            angryIntensity > 0.5 -> """
                语气指导：
                - 语气比较直接
                - 稍微有点冷淡
                - 但还是愿意帮忙
            """.trimIndent()

            else -> """
                语气指导：
                - 用正常、友好的语气
                - 保持自然、真实
            """.trimIndent()
        }
    }
}
