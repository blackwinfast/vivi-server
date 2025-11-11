package com.example

import com.example.services.AnniversaryService
import com.example.services.AnniversaryUI
import com.example.services.CreatorType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnniversaryServiceSpec {

    @Test
    fun testAddAndGetTodayAnniversary() {
        val service = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "ann_user"
        val today = LocalDate.of(2025, 11, 10)

        val ann = service.addAnniversary(
            userId = userId,
            title = "TestAnn",
            date = today,
            createdBy = CreatorType.USER,
            description = "desc",
            recurring = true,
        )

        val todays = service.getTodayAnniversaries(userId, today)
        assertTrue(todays.any { it.id == ann.id })

        val boost = service.getEmotionBoost(todays)
        assertEquals(0.3, boost)

        val msg = service.generateAnniversaryMessage(todays)
        assertTrue(msg != null && msg.contains("TestAnn"))
    }

    @Test
    fun testGetUpcomingAnniversaries() {
        val service = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "ann2"
        val today = LocalDate.of(2025, 11, 10)
        val in3 = today.plusDays(3)
        service.addAnniversary(userId, "A1", in3, CreatorType.USER, recurring = false)
        service.addAnniversary(userId, "A2", today.plusDays(10), CreatorType.AI, recurring = true)

        val all = service.getUserAnniversaries(userId)
        println("DEBUG all anns: $all")
        val upcoming7 = service.getUpcomingAnniversaries(userId, days = 7, today = today)
        val titles7 = upcoming7.map { it.title }
        println("DEBUG upcoming7: $upcoming7 | titles: $titles7")
        assertTrue(titles7.contains("A1"), "titles7: $titles7")

        val upcoming30 = service.getUpcomingAnniversaries(userId, days = 30, today = today)
        val titles30 = upcoming30.map { it.title }
        println("DEBUG upcoming30: $upcoming30 | titles: $titles30")
        assertTrue(titles30.contains("A2"), "titles30: $titles30")
    }

    @Test
    fun testAnniversaryUIMappings() {
        val service = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val ui = AnniversaryUI(service)
        val userId = "ui_user"
        val today = LocalDate.of(2025, 11, 10)
        service.addAnniversary(userId, "UI Test", today, CreatorType.AI, description = "desc", recurring = true)

        val msgs = ui.getTodayMessages(userId, today)
        assertTrue(msgs.isNotEmpty())
        assertTrue(msgs[0].contains("UI Test"))

        val summaries = ui.getUpcomingSummaries(userId, days = 7, today = today)
        println("DEBUG summaries: $summaries")
        assertTrue(summaries.isNotEmpty())
        val item = summaries[0]
        assertTrue(item.containsKey("id"))
        assertTrue(item.containsKey("title"))
    }
}
