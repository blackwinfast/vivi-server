package com.example.services

import com.example.models.EmotionType
import com.example.models.UserEmotionStates
import java.time.LocalDateTime

/**
 * Emotion display helper — 渲染使用者/微微的情緒狀態成易於前端顯示的資料
 */
class EmotionDisplay {

    /**
     * 根據目前情緒狀態渲染一個摘要與語氣建議
     * 可選擇性接收今天的紀念日清單，並回傳相關訊息與 boost
     */
    fun renderEmotion(
        userEmotions: UserEmotionStates,
        userId: String,
        todayAnniversaries: List<Anniversary> = emptyList(),
        now: LocalDateTime = LocalDateTime.now(),
    ): Map<String, Any> {
        val primary = userEmotions.getPrimaryEmotion(now) ?: EmotionType.NEUTRAL
        val intensity = userEmotions.getIntensity(primary, now)

        // 所有非零情緒
        val all = userEmotions.emotions.mapValues { it.value.getCurrentIntensity(now) }
            .filter { it.value > 0.0 }
            .mapValues { (_, v) -> v }

        val summary = when (primary) {
            EmotionType.HAPPY -> when {
                intensity > 0.8 -> "微微現在非常高興，語氣會比較興奮與撒嬌。"
                intensity > 0.5 -> "微微心情不錯，語氣比較輕鬆友好。"
                intensity > 0.2 -> "微微有點高興，但不會太明顯。"
                else -> "微微情緒偏中性或低度高興。"
            }
            EmotionType.ANGRY -> when {
                intensity > 0.8 -> "微微現在有點生氣，語氣會比較冷淡、直接，但仍願意幫忙。"
                intensity > 0.5 -> "微微有些不爽，語氣會比較直接。"
                intensity > 0.2 -> "微微稍微不滿，但影響不大。"
                else -> "微微情緒偏中性或低度不滿。"
            }
            EmotionType.SAD -> when {
                intensity > 0.5 -> "微微現在有點難過，語氣會比較溫和且有同理心。"
                intensity > 0.2 -> "微微稍微有點低落。"
                else -> "微微情緒偏中性。"
            }
            EmotionType.EMERGENCY -> "注意：检测到紧急情绪，请优先处理。"
            else -> "微微目前情緒中性。"
        }

        val toneGuidance = when {
            primary == EmotionType.HAPPY && intensity > 0.8 -> "excited"
            primary == EmotionType.HAPPY && intensity > 0.5 -> "friendly"
            primary == EmotionType.ANGRY && intensity > 0.8 -> "cold_direct"
            primary == EmotionType.ANGRY && intensity > 0.5 -> "direct"
            primary == EmotionType.SAD && intensity > 0.5 -> "soft_empathic"
            else -> "neutral"
        }

        // 處理紀念日輸出
        val anniversaryMessages = todayAnniversaries.map { ann ->
            val creator = if (ann.createdBy == CreatorType.AI) "我（微微）" else "你"
            val desc = ann.description?.let { "（$it）" } ?: ""
            "今天是${creator}设定的「${ann.title}」$desc"
        }

        val anniversaryBoost = if (todayAnniversaries.isEmpty()) 0.0 else todayAnniversaries.size * 0.3

        return mapOf(
            "primary" to primary.name,
            "intensity" to intensity,
            "summary" to summary,
            "tone" to toneGuidance,
            "all" to all,
            "anniversary_messages" to anniversaryMessages,
            "anniversary_boost" to anniversaryBoost,
            "anniversaries" to todayAnniversaries.map { ann ->
                mapOf(
                    "id" to ann.id,
                    "title" to ann.title,
                    "date" to ann.date.toString(),
                    "createdBy" to ann.createdBy.name,
                    "description" to (ann.description ?: ""),
                    "recurring" to ann.recurring,
                )
            },
        )
    }
}
