package com.example

import com.example.repositories.DatabaseFactory
import com.example.repositories.SqliteAnniversaryDao
import com.example.repositories.SqliteEmotionMemoryDao
import com.example.services.CreatorType
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqliteDaoTests {

    @Test
    fun testAnniversarySqliteCrud() {
        val jdbc = "jdbc:sqlite:file:memann?mode=memory&cache=shared"
        DatabaseFactory.connect(jdbc)
        val dao = SqliteAnniversaryDao(jdbc)

        val ann = com.example.services.Anniversary(
            id = "a1",
            userId = "u1",
            title = "SAnn",
            date = LocalDate.of(2025, 11, 10),
            createdBy = CreatorType.USER,
            description = "d",
            recurring = true,
        )

        dao.save(ann)
        val list = dao.listByUser("u1")
        assertEquals(1, list.size)
        val fetched = dao.findById("a1")
        assertNotNull(fetched)

        val updated = ann.copy(title = "SAnn2")
        val ok = dao.update(updated)
        assertTrue(ok)
        val fetched2 = dao.findById("a1")
        assertEquals("SAnn2", fetched2?.title)

        val del = dao.delete("a1")
        assertTrue(del)
        val after = dao.listByUser("u1")
        assertEquals(0, after.size)
    }

    @Test
    fun testEmotionMemorySqlite() {
        val jdbc = "jdbc:sqlite:file:mememo?mode=memory&cache=shared"
        DatabaseFactory.connect(jdbc)
        val dao = SqliteEmotionMemoryDao(jdbc)

        val now = LocalDateTime.of(2025, 11, 10, 12, 0)
        val entry = com.example.models.EmotionMemoryEntry(
            event = "ev1",
            emotionType = com.example.models.EmotionType.HAPPY,
            intensity = 0.5,
            timestamp = now,
            eventType = com.example.models.EventType.PRAISE,
        )

        dao.addEntry("u1", entry)
        val list = dao.listEntries("u1")
        assertEquals(1, list.size)

        val count = dao.getEventCount("u1", "ev1")
        assertEquals(1, count)

        val last = dao.getLastEventTime("u1", "ev1")
        assertNotNull(last)
    }
}
