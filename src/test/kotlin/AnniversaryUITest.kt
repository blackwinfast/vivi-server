package com.example

import com.example.services.AnniversaryService
import com.example.services.AnniversaryUI
import com.example.services.CreatorType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnniversaryUITest {

    @Test
    fun testGetTodayMessagesAndUpcomingSummaries() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val ui = AnniversaryUI(svc)
        val userId = "ui_user"
        val today = LocalDate.of(2025, 11, 10)

        svc.addAnniversary(userId, "UIAnn", today, CreatorType.AI, description = "demo", recurring = true)

        val msgs = ui.getTodayMessages(userId, today)
        assertTrue(msgs.isNotEmpty(), "msgs empty: $msgs")
        assertTrue(msgs.first().contains("UIAnn"), "first msg: ${msgs.firstOrNull()}")

        val summaries = ui.getUpcomingSummaries(userId, days = 7, today = today)
        assertTrue(summaries.isNotEmpty(), "summaries empty: $summaries")
        val first = summaries.first()
        assertEquals("UIAnn", first["title"], "first summary: $first")
    }
}
