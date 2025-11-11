package com.example

import com.example.services.AnniversaryService
import com.example.services.CreatorType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class UpcomingAnniversaryTest {
    @Test
    fun testNextOccurrenceRecurringAndNonRecurring() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "u_test"
        val base = LocalDate.of(2025, 11, 10)

        // recurring anniversary on Nov 10 (today)
        svc.addAnniversary(userId, "RecurringToday", LocalDate.of(2000, 11, 10), CreatorType.AI, recurring = true)
        // recurring anniversary on Nov 13
        svc.addAnniversary(userId, "RecurringLater", LocalDate.of(2000, 11, 13), CreatorType.AI, recurring = true)
        // non-recurring future (Nov 13, 2025)
        svc.addAnniversary(userId, "OneTimeFuture", LocalDate.of(2025, 11, 13), CreatorType.USER, recurring = false)
        // non-recurring past (Nov 1, 2024) should be ignored
        svc.addAnniversary(userId, "OneTimePast", LocalDate.of(2024, 11, 1), CreatorType.USER, recurring = false)

        val upcoming7 = svc.getUpcomingAnniversaries(userId, days = 7, today = base)
        val titles = upcoming7.map { it.title }

        // Expect RecurringToday (0 days ahead) and RecurringLater / OneTimeFuture (3 days ahead)
        assertTrue(titles.contains("RecurringToday"), "Expected RecurringToday in upcoming7, got $titles")
        assertTrue(titles.contains("RecurringLater") || titles.contains("OneTimeFuture"), "Expected a Nov13 anniversary in upcoming7, got $titles")

        val upcoming30 = svc.getUpcomingAnniversaries(userId, days = 30, today = base)
        val titles30 = upcoming30.map { it.title }
        assertTrue(titles30.contains("OneTimeFuture") || titles30.contains("RecurringLater"), "Expected Nov13 in upcoming30, got $titles30")
        assertTrue(!titles30.contains("OneTimePast"), "Did not expect OneTimePast (past) to appear: $titles30")
    }
}
