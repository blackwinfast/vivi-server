package com.example.models

import java.time.LocalDateTime

/**
 * 情绪状态数据类
 * 记录情绪的强度、持续时间、衰减速度
 * 注意：LocalDateTime 不直接序列化，使用字符串存储
 */
data class EmotionState(
    val type: EmotionType,
    var intensity: Double, // 情绪强度（0.0 - 无上限，高兴和生气可以很高）
    var lastUpdated: LocalDateTime,
    val decayRate: Double, // 衰减速度（每小时衰减的强度）
) {
    /**
     * 计算当前强度（考虑时间衰减）
     */
    fun getCurrentIntensity(now: LocalDateTime = LocalDateTime.now()): Double {
        val hoursPassed = java.time.Duration.between(lastUpdated, now).toHours()
        val decayed = intensity - (decayRate * hoursPassed)
        return maxOf(0.0, decayed) // 强度不能为负
    }

    /**
     * 更新强度并记录时间
     */
    fun updateIntensity(newIntensity: Double, now: LocalDateTime = LocalDateTime.now()) {
        intensity = newIntensity
        lastUpdated = now
    }

    /**
     * 应用时间衰减
     */
    fun applyDecay(now: LocalDateTime = LocalDateTime.now()) {
        val currentIntensity = getCurrentIntensity(now)
        updateIntensity(currentIntensity, now)
    }
}

/**
 * 用户情绪状态集合
 * 每个用户可以有多个情绪状态（例如同时高兴和生气）
 */
data class UserEmotionStates(
    val userId: String,
    val emotions: MutableMap<EmotionType, EmotionState> = mutableMapOf(),
) {
    /**
     * 获取主要情绪（强度最高的）
     */
    fun getPrimaryEmotion(now: LocalDateTime = LocalDateTime.now()): EmotionType? {
        return emotions.entries
            .maxByOrNull { it.value.getCurrentIntensity(now) }
            ?.key
    }

    /**
     * 获取情绪强度
     */
    fun getIntensity(emotionType: EmotionType, now: LocalDateTime = LocalDateTime.now()): Double {
        return emotions[emotionType]?.getCurrentIntensity(now) ?: 0.0
    }

    /**
     * 更新或创建情绪状态
     */
    fun updateEmotion(
        emotionType: EmotionType,
        intensityChange: Double,
        decayRate: Double = getDefaultDecayRate(emotionType),
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val existing = emotions[emotionType]
        if (existing != null) {
            existing.applyDecay(now) // 先应用衰减
            val newIntensity = existing.intensity + intensityChange
            existing.updateIntensity(maxOf(0.0, newIntensity), now)
        } else {
            emotions[emotionType] = EmotionState(
                type = emotionType,
                intensity = maxOf(0.0, intensityChange),
                lastUpdated = now,
                decayRate = decayRate,
            )
        }
    }

    /**
     * 获取默认衰减速度
     */
    private fun getDefaultDecayRate(emotionType: EmotionType): Double {
        return when (emotionType) {
            EmotionType.HAPPY -> 0.05 // 高兴衰减较慢（可持续数天）
            EmotionType.ANGRY -> 0.03 // 生气衰减很慢（可持续一周以上）
            EmotionType.SAD -> 0.1 // 悲伤衰减较快
            EmotionType.NEUTRAL -> 1.0 // 中性快速衰减
            EmotionType.EMERGENCY -> 0.5 // 紧急情况衰减中等
        }
    }
}
