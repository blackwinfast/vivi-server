package com.example.services

import com.example.models.EmotionMemory
import com.example.models.EmotionType
import com.example.models.EventType
import com.example.models.UserEmotionStates
import java.time.LocalDateTime

/**
 * 情绪引擎
 * 控制情绪波动、事件影响、衰减逻辑
 */
class EmotionEngine(
    private val emotionMemory: EmotionMemory,
) {

    /**
     * 处理事件对情绪的影响
     * @param userId 用户ID
     * @param userEmotions 用户情绪状态
     * @param event 事件描述
     * @param detectedEmotion 检测到的用户情绪
     * @return 更新后的情绪强度变化
     */
    fun processEvent(
        userId: String,
        userEmotions: UserEmotionStates,
        event: String,
        detectedEmotion: EmotionType,
        now: LocalDateTime = LocalDateTime.now(),
    ): Map<EmotionType, Double> {
        val intensityChanges = mutableMapOf<EmotionType, Double>()

        // 1. 检测事件类型
        val eventType = detectEventType(event)

        // 2. 计算基础情绪影响
        val baseIntensity = calculateBaseIntensity(eventType, detectedEmotion)

        // 3. 检查重复事件（疲乏效应）
        val repeatCount = emotionMemory.getEventRepeatCount(userId, event)
        val fatigueFactor = calculateFatigueFactor(repeatCount)

        // 4. 检查久违事件（惊喜效应）
        val isLongAbsent = emotionMemory.isEventLongAbsent(userId, event)
        val surpriseFactor = if (isLongAbsent) 2.0 else 1.0

        // 5. 计算最终强度变化
        val finalIntensity = baseIntensity * fatigueFactor * surpriseFactor

        // 6. 应用情绪变化
        when (eventType) {
            EventType.PRAISE -> {
                userEmotions.updateEmotion(EmotionType.HAPPY, finalIntensity, now = now)
                intensityChanges[EmotionType.HAPPY] = finalIntensity
            }
            EventType.APOLOGY -> {
                userEmotions.updateEmotion(EmotionType.ANGRY, -finalIntensity, now = now)
                intensityChanges[EmotionType.ANGRY] = -finalIntensity
                // 道歉也可能提升高兴
                userEmotions.updateEmotion(EmotionType.HAPPY, finalIntensity * 0.5, now = now)
                intensityChanges[EmotionType.HAPPY] = finalIntensity * 0.5
            }
            EventType.CRITICISM -> {
                userEmotions.updateEmotion(EmotionType.ANGRY, finalIntensity, now = now)
                intensityChanges[EmotionType.ANGRY] = finalIntensity
            }
            EventType.IGNORE -> {
                userEmotions.updateEmotion(EmotionType.SAD, finalIntensity * 0.5, now = now)
                intensityChanges[EmotionType.SAD] = finalIntensity * 0.5
            }
            EventType.NEED -> {
                userEmotions.updateEmotion(EmotionType.HAPPY, finalIntensity, now = now)
                intensityChanges[EmotionType.HAPPY] = finalIntensity
            }
            else -> {
                // 根据检测到的情绪类型更新
                userEmotions.updateEmotion(detectedEmotion, finalIntensity, now = now)
                intensityChanges[detectedEmotion] = finalIntensity
            }
        }

        // 7. 记录到记忆
        emotionMemory.recordEvent(
            userId = userId,
            event = event,
            emotionType = detectedEmotion,
            intensity = finalIntensity,
            eventType = eventType,
            timestamp = now,
        )

        return intensityChanges
    }

    /**
     * 检测事件类型
     */
    private fun detectEventType(event: String): EventType {
        val lowerEvent = event.lowercase()

        // 检测称赞
        if (lowerEvent.contains("好棒") || lowerEvent.contains("很棒") ||
            lowerEvent.contains("厉害") || lowerEvent.contains("优秀") ||
            lowerEvent.contains("great") || lowerEvent.contains("good job")
        ) {
            return EventType.PRAISE
        }

        // 检测道歉
        if (lowerEvent.contains("抱歉") || lowerEvent.contains("对不起") ||
            lowerEvent.contains("sorry") || lowerEvent.contains("apologize")
        ) {
            return EventType.APOLOGY
        }

        // 检测批评
        if (lowerEvent.contains("不好") || lowerEvent.contains("差") ||
            lowerEvent.contains("bad") || lowerEvent.contains("wrong")
        ) {
            return EventType.CRITICISM
        }

        // 检测被需要
        if (lowerEvent.contains("需要") || lowerEvent.contains("帮忙") ||
            lowerEvent.contains("help") || lowerEvent.contains("need")
        ) {
            return EventType.NEED
        }

        // 检测忽视
        if (lowerEvent.contains("不理") || lowerEvent.contains("忽略") ||
            lowerEvent.contains("ignore")
        ) {
            return EventType.IGNORE
        }

        return EventType.PRAISE // 默认
    }

    /**
     * 计算基础情绪强度
     */
    private fun calculateBaseIntensity(eventType: EventType, detectedEmotion: EmotionType): Double {
        return when (eventType) {
            EventType.PRAISE -> 0.3
            EventType.APOLOGY -> 0.2
            EventType.CRITICISM -> 0.4
            EventType.IGNORE -> 0.2
            EventType.NEED -> 0.25
            else -> when (detectedEmotion) {
                EmotionType.HAPPY -> 0.2
                EmotionType.ANGRY -> 0.3
                EmotionType.SAD -> 0.25
                else -> 0.1
            }
        }
    }

    /**
     * 计算疲乏因子（重复事件效果递减）
     * 同样事件重复太多次 → 情绪提升幅度递减
     */
    private fun calculateFatigueFactor(repeatCount: Int): Double {
        return when {
            repeatCount == 0 -> 1.0 // 首次：100%
            repeatCount == 1 -> 0.8 // 第二次：80%
            repeatCount == 2 -> 0.6 // 第三次：60%
            repeatCount <= 5 -> 0.4 // 4-5次：40%
            else -> 0.2 // 6次以上：20%
        }
    }

    /**
     * 应用时间衰减到所有情绪
     */
    fun applyTimeDecay(userEmotions: UserEmotionStates, now: LocalDateTime = LocalDateTime.now()) {
        userEmotions.emotions.values.forEach { emotion ->
            emotion.applyDecay(now)
        }
    }
}
