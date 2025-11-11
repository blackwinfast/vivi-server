package com.example

import com.example.models.EmotionMemory
import com.example.models.EmotionType
import com.example.services.EmotionEngine
import com.example.services.UserEmotionService
import java.time.LocalDateTime

/**
 * 测试微微情绪系统
 */
fun main() {
    println("=== 微微情绪系统测试 ===\n")

    // 1. 初始化服务
    val emotionMemory = EmotionMemory(com.example.repositories.InMemoryEmotionMemoryDao())
    val emotionEngine = EmotionEngine(emotionMemory)
    val userEmotionService = UserEmotionService()

    // 2. 获取或创建用户情绪状态
    val userId = "test_user"
    val userEmotions = userEmotionService.getOrCreateUserEmotions(userId)
    val now = LocalDateTime.now()

    // 3. 设置初始情绪：HAPPY，强度 0.7
    userEmotions.updateEmotion(EmotionType.HAPPY, 0.7, now = now)
    println("初始状态：")
    println("  情绪类型：${EmotionType.HAPPY}")
    println("  强度：${userEmotions.getIntensity(EmotionType.HAPPY, now)}")
    println()

    // 4. 测试：提升情绪（称赞）
    println("测试1：用户称赞微微")
    val intensityChanges1 = emotionEngine.processEvent(
        userId = userId,
        userEmotions = userEmotions,
        event = "微微你好棒！",
        detectedEmotion = EmotionType.HAPPY,
        now = now,
    )
    println("  情绪变化：$intensityChanges1")
    println("  当前高兴强度：${userEmotions.getIntensity(EmotionType.HAPPY, now)}")
    println()

    // 5. 测试：应用时间衰减（模拟1小时后）
    println("测试2：1小时后应用时间衰减")
    val oneHourLater = now.plusHours(1)
    emotionEngine.applyTimeDecay(userEmotions, oneHourLater)
    println("  1小时后的高兴强度：${userEmotions.getIntensity(EmotionType.HAPPY, oneHourLater)}")
    println()

    // 6. 测试：重复事件（疲乏效应）
    println("测试3：重复称赞（疲乏效应）")
    val intensityChanges2 = emotionEngine.processEvent(
        userId = userId,
        userEmotions = userEmotions,
        event = "微微你好棒！",
        detectedEmotion = EmotionType.HAPPY,
        now = oneHourLater,
    )
    println("  情绪变化：$intensityChanges2")
    println("  当前高兴强度：${userEmotions.getIntensity(EmotionType.HAPPY, oneHourLater)}")
    println()

    // 7. 测试：获取主要情绪
    println("测试4：获取主要情绪")
    val primaryEmotion = userEmotions.getPrimaryEmotion(oneHourLater)
    println("  主要情绪：$primaryEmotion")
    println("  主要情绪强度：${primaryEmotion?.let { userEmotions.getIntensity(it, oneHourLater) }}")
    println()

    // 8. 测试：道歉修复
    println("测试5：用户道歉（修复生气情绪）")
    // 先让微微生气
    userEmotions.updateEmotion(EmotionType.ANGRY, 0.5, now = oneHourLater)
    println("  生气强度（道歉前）：${userEmotions.getIntensity(EmotionType.ANGRY, oneHourLater)}")

    val intensityChanges3 = emotionEngine.processEvent(
        userId = userId,
        userEmotions = userEmotions,
        event = "抱歉，刚才说错了",
        detectedEmotion = EmotionType.NEUTRAL,
        now = oneHourLater,
    )
    println("  情绪变化：$intensityChanges3")
    println("  生气强度（道歉后）：${userEmotions.getIntensity(EmotionType.ANGRY, oneHourLater)}")
    println("  高兴强度（道歉后）：${userEmotions.getIntensity(EmotionType.HAPPY, oneHourLater)}")
    println()

    // 9. 最终状态
    println("=== 最终状态 ===")
    val finalEmotion = userEmotions.getPrimaryEmotion(oneHourLater)
    val finalIntensity = finalEmotion?.let { userEmotions.getIntensity(it, oneHourLater) } ?: 0.0
    println("微微現在的情緒是：$finalEmotion，強度：$finalIntensity")

    // 显示所有情绪状态
    println("\n所有情绪状态：")
    userEmotions.emotions.forEach { (type, state) ->
        val currentIntensity = state.getCurrentIntensity(oneHourLater)
        if (currentIntensity > 0.0) {
            println("  $type: $currentIntensity (衰减速度: ${state.decayRate}/小时)")
        }
    }
}
