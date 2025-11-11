package com.example.services

import com.example.models.EmotionType
import com.example.models.UserEmotionStates
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户情绪状态管理服务
 * 管理每个用户的情绪状态
 */
class UserEmotionService {

    // 用户情绪状态存储（实际应该使用数据库）
    private val userEmotionStates = ConcurrentHashMap<String, UserEmotionStates>()

    /**
     * 获取或创建用户的情绪状态
     */
    fun getOrCreateUserEmotions(userId: String): UserEmotionStates {
        return userEmotionStates.getOrPut(userId) {
            UserEmotionStates(userId = userId)
        }
    }

    /**
     * 获取用户情绪状态
     */
    fun getUserEmotions(userId: String): UserEmotionStates? {
        return userEmotionStates[userId]
    }

    /**
     * 应用时间衰减到所有用户的情绪
     */
    fun applyTimeDecayToAll(now: LocalDateTime = LocalDateTime.now()) {
        userEmotionStates.values.forEach { userEmotions ->
            userEmotions.emotions.values.forEach { emotion ->
                emotion.applyDecay(now)
            }
        }
    }

    /**
     * 获取用户当前主要情绪
     */
    fun getPrimaryEmotion(userId: String, now: LocalDateTime = LocalDateTime.now()): EmotionType? {
        return getUserEmotions(userId)?.getPrimaryEmotion(now)
    }

    /**
     * 获取用户情绪强度
     */
    fun getEmotionIntensity(userId: String, emotionType: EmotionType, now: LocalDateTime = LocalDateTime.now()): Double {
        return getUserEmotions(userId)?.getIntensity(emotionType, now) ?: 0.0
    }
}
