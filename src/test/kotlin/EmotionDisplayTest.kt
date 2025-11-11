package com.example

import com.example.models.EmotionMemory
import com.example.models.EmotionType
import com.example.models.EventType
import com.example.models.UserEmotionStates
import com.example.services.AnniversaryService
import com.example.services.CreatorType
import com.example.services.EmotionDisplay
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmotionDisplayTest {

    @Test
    fun testRenderWithoutAnniversaries() {
        val userEmotions = UserEmotionStates("u1")
        val now = LocalDateTime.of(2025, 11, 10, 12, 0)
        userEmotions.updateEmotion(EmotionType.HAPPY, 0.7, now = now)

        val display = EmotionDisplay()
        val rendered = display.renderEmotion(userEmotions, "u1", emptyList(), now)

        assertEquals("HAPPY", rendered["primary"])
        assertEquals(0.7, rendered["intensity"])
        assertEquals(0.0, rendered["anniversary_boost"])
        val annMsgs = rendered["anniversary_messages"] as? List<*>
        assertTrue(annMsgs != null && annMsgs.isEmpty())
    }

    @Test
    fun testRenderWithAnniversary() {
        val userId = "u2"
        val annService = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val today = LocalDate.of(2025, 11, 10)
        annService.addAnniversary(userId, "測試紀念日", today, CreatorType.AI, description = "demo", recurring = true)

        val todayAnns = annService.getTodayAnniversaries(userId, today)
        assertTrue(todayAnns.isNotEmpty())

        val userEmotions = UserEmotionStates(userId)
        val now = LocalDateTime.of(2025, 11, 10, 12, 0)
        userEmotions.updateEmotion(EmotionType.NEUTRAL, 0.0, now = now)

        val display = EmotionDisplay()
        val rendered = display.renderEmotion(userEmotions, userId, todayAnns, now)

        val boost = rendered["anniversary_boost"] as? Double
        assertEquals(0.3, boost)
        val msgs = rendered["anniversary_messages"] as? List<*>
        assertTrue(msgs != null && msgs.isNotEmpty())
    }

    @Test
    fun testReplayAndDisplayIntegration() {
        val userId = "u3"
        val memory = EmotionMemory(com.example.repositories.InMemoryEmotionMemoryDao())
        val now = LocalDateTime.of(2025, 11, 10, 12, 0)

        // record a memory
        memory.recordEvent(userId = userId, event = "用户说你好棒", emotionType = EmotionType.HAPPY, intensity = 0.6, eventType = EventType.PRAISE, timestamp = now.minusDays(1))

        // replay
        val mem = memory.replayEmotion(userId, "你好棒")
        assertTrue(mem != null)

        val userEmotions = UserEmotionStates(userId)
        userEmotions.updateEmotion(mem!!.emotionType, mem.intensity, now = now)

        val display = EmotionDisplay()
        val rendered = display.renderEmotion(userEmotions, userId, emptyList(), now)

        assertEquals("HAPPY", rendered["primary"])
        assertEquals(0.6, rendered["intensity"])
    }
}
