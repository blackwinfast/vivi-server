package com.example

import com.example.models.EmotionMemory
import com.example.models.EmotionType
import com.example.models.EventType
import com.example.models.UserEmotionStates
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EmotionReplayTest {

    @Test
    fun testEmotionMemoryReplayAndApply() {
        val memory = EmotionMemory(com.example.repositories.InMemoryEmotionMemoryDao())
        val userId = "test_user"
        val now = LocalDateTime.of(2025, 11, 10, 12, 0)

        // Record an event in the past
        memory.recordEvent(
            userId = userId,
            event = "用户说你好棒",
            emotionType = EmotionType.HAPPY,
            intensity = 0.6,
            eventType = EventType.PRAISE,
            timestamp = now.minusDays(1),
        )

        // Replay by keyword
        val mem = memory.replayEmotion(userId, "你好棒")
        assertNotNull(mem, "Expected to find a replayable memory")
        assertEquals("用户说你好棒", mem!!.event)

        // Apply to user emotions
        val userEmotions = UserEmotionStates(userId)
        userEmotions.updateEmotion(mem.emotionType, mem.intensity, now = now)

        val intensity = userEmotions.getIntensity(mem.emotionType, now)
        assertEquals(mem.intensity, intensity, "Applied intensity should match memory intensity")
    }
}
