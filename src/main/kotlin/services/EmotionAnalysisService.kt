package com.example.services

import com.example.models.EmotionType

/**
 * 情绪分析服务
 * 这里使用简单的关键词匹配作为示例
 * 实际生产环境应该使用 Google NLP、IBM Watson 等专业服务
 */
class EmotionAnalysisService {

    // 紧急关键词列表（需要立即处理的极端负面内容）
    private val emergencyKeywords = listOf(
        "不想活了", "自杀", "结束生命", "不想活了", "死了算了",
        "kill myself", "suicide", "end my life", "want to die",
    )

    // 悲伤关键词
    private val sadKeywords = listOf(
        "难过", "伤心", "悲伤", "沮丧", "失落", "痛苦", "哭",
        "sad", "depressed", "unhappy", "upset", "down", "cry",
    )

    // 愤怒关键词
    private val angryKeywords = listOf(
        "生气", "愤怒", "气死了", "讨厌", "恨", "烦",
        "angry", "mad", "furious", "hate", "annoyed",
    )

    // 快乐关键词
    private val happyKeywords = listOf(
        "开心", "高兴", "快乐", "兴奋", "愉快",
        "happy", "glad", "excited", "joyful", "pleased",
    )

    /**
     * 分析文本情绪
     * @param text 输入文本
     * @return 情绪类型
     */
    suspend fun analyzeEmotion(text: String): EmotionType {
        if (text.isBlank()) {
            return EmotionType.NEUTRAL
        }

        val lowerText = text.lowercase()
        val trimmedText = text.trim()

        // 首先检查是否为紧急情况（最高优先级）
        val emergencyMatches = emergencyKeywords.count { keyword ->
            lowerText.contains(keyword, ignoreCase = true)
        }
        if (emergencyMatches > 0) {
            return EmotionType.EMERGENCY
        }

        // 计算各情绪关键词的匹配数量和强度
        val sadCount = sadKeywords.count { lowerText.contains(it, ignoreCase = true) }
        val angryCount = angryKeywords.count { lowerText.contains(it, ignoreCase = true) }
        val happyCount = happyKeywords.count { lowerText.contains(it, ignoreCase = true) }

        // 计算情绪强度（考虑重复出现和文本长度）
        val textLength = text.length
        val sadIntensity = if (textLength > 0) (sadCount.toDouble() / textLength) * 1000 else 0.0
        val angryIntensity = if (textLength > 0) (angryCount.toDouble() / textLength) * 1000 else 0.0
        val happyIntensity = if (textLength > 0) (happyCount.toDouble() / textLength) * 1000 else 0.0

        // 根据匹配数量和强度判断情绪
        return when {
            // 悲伤情绪优先级
            sadCount > 0 && (sadCount > angryCount || sadIntensity > angryIntensity) &&
                (sadCount > happyCount || sadIntensity > happyIntensity) -> EmotionType.SAD

            // 愤怒情绪优先级
            angryCount > 0 && (angryCount > sadCount || angryIntensity > sadIntensity) &&
                (angryCount > happyCount || angryIntensity > happyIntensity) -> EmotionType.ANGRY

            // 快乐情绪
            happyCount > 0 && happyIntensity > 0.5 -> EmotionType.HAPPY

            // 默认中性
            else -> EmotionType.NEUTRAL
        }
    }

    /**
     * 使用外部 API 进行情绪分析（示例：可以集成 Google NLP 等）
     * 这里提供一个接口，实际使用时需要替换为真实的 API 调用
     */
    private suspend fun analyzeWithExternalAPI(text: String): EmotionType {
        // TODO: 集成真实的情绪分析 API
        // 例如：Google Cloud Natural Language API
        // 或 IBM Watson Tone Analyzer
        return analyzeEmotion(text) // 暂时使用本地分析
    }
}
