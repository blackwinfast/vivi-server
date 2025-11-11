package com.example

import com.example.services.AnniversaryService
import com.example.services.CreatorType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnniversaryBoundaryTests {

    @Test
    fun testCrossYearUpcoming() {
        val svc = AnniversaryService(debugEnabled = true, anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "cross_user"
        // recurring Jan 1 and Dec 31
        svc.addAnniversary(userId, "YearEnd", LocalDate.of(2000, 12, 31), CreatorType.USER, recurring = true)
        svc.addAnniversary(userId, "NewYear", LocalDate.of(2000, 1, 1), CreatorType.USER, recurring = true)

        // today is Dec 30, expect YearEnd (Dec31) and NewYear (Jan1) within next 3 days
        val today = LocalDate.of(2025, 12, 30)
        val upcoming3 = svc.getUpcomingAnniversaries(userId, days = 3, today = today)
        val titles = upcoming3.map { it.title }
        assertTrue(titles.contains("YearEnd"), "Expected YearEnd in upcoming3: ${svc.lastDebug}")
        assertTrue(titles.contains("NewYear"), "Expected NewYear in upcoming3: ${svc.lastDebug}")
    }

    @Test
    fun testMultipleSameDayAnniversaries() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "multi_user"
        val day = LocalDate.of(2025, 11, 10)
        svc.addAnniversary(userId, "A1", day, CreatorType.USER, recurring = true)
        svc.addAnniversary(userId, "A2", day, CreatorType.AI, recurring = false)
        svc.addAnniversary(userId, "A3", day, CreatorType.USER, recurring = true)

        val upcoming = svc.getUpcomingAnniversaries(userId, days = 1, today = day)
        val titles = upcoming.map { it.title }
        assertTrue(titles.containsAll(listOf("A1", "A2", "A3")), "Expected all three on same day: $titles")
    }

    @Test
    fun testNonRecurringPastIgnored() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "past_user"
        svc.addAnniversary(userId, "Past", LocalDate.of(2024, 5, 1), CreatorType.USER, recurring = false)
        val today = LocalDate.of(2025, 11, 10)
        val upcoming = svc.getUpcomingAnniversaries(userId, days = 30, today = today)
        val titles = upcoming.map { it.title }
        assertFalse(titles.contains("Past"), "Past non-recurring should not appear: $titles")
    }
}
