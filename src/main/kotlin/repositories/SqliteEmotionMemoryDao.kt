package com.example.repositories

import com.example.models.EmotionMemoryEntry
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object EmotionMemoriesTable : Table("emotion_memories") {
    // Use a string id (UUID) instead of DB auto-increment to avoid dialect-specific
    // auto-increment constraints on SQLite in this patch. IDs are generated in the DAO.
    val id = varchar("id", 200)
    val userId = varchar("user_id", 200)
    val event = varchar("event", 1000)
    val emotionType = varchar("emotion_type", 64)
    val intensity = double("intensity")
    val timestamp = varchar("timestamp", 64)
    val eventType = varchar("event_type", 64)
}

class SqliteEmotionMemoryDao(private val jdbcUrl: String = "jdbc:sqlite:data/dev.db") : EmotionMemoryDao {
    init {
        DatabaseFactory.connect(jdbcUrl)
    }

    override fun addEntry(userId: String, entry: EmotionMemoryEntry) {
        transaction {
            try {
                val genId = "${'$'}{userId}_${'$'}{System.currentTimeMillis()}_${'$'}{java.util.UUID.randomUUID()}"
                EmotionMemoriesTable.insert {
                    it[EmotionMemoriesTable.id] = genId
                    it[EmotionMemoriesTable.userId] = userId
                    it[event] = entry.event
                    it[emotionType] = entry.emotionType.name
                    it[intensity] = entry.intensity
                    it[timestamp] = entry.timestamp.toString()
                    it[eventType] = entry.eventType.name
                }
            } catch (e: ExposedSQLException) {
                println("SqliteEmotionMemoryDao.addEntry insert failed: ${'$'}{e::class} ${'$'}{e.message}")
                throw e
            }
        }
    }

    override fun listEntries(userId: String): List<EmotionMemoryEntry> {
        return transaction {
            EmotionMemoriesTable.selectAll()
                .asSequence()
                .filter { row -> row.get(EmotionMemoriesTable.userId) == userId }
                .map { row ->
                    EmotionMemoryEntry(
                        event = row.get(EmotionMemoriesTable.event),
                        emotionType = com.example.models.EmotionType.valueOf(row.get(EmotionMemoriesTable.emotionType)),
                        intensity = row.get(EmotionMemoriesTable.intensity),
                        timestamp = java.time.LocalDateTime.parse(row.get(EmotionMemoriesTable.timestamp)),
                        eventType = com.example.models.EventType.valueOf(row.get(EmotionMemoriesTable.eventType)),
                    )
                }.toList()
        }
    }

    override fun incrementEventCount(userId: String, event: String) {
        // For DB-backed implementation we don't keep a separate counter; counts can be derived from rows.
        // No-op for performance; getEventCount will query the table.
    }

    override fun getEventCount(userId: String, event: String): Int {
        return transaction {
            EmotionMemoriesTable.selectAll()
                .asSequence()
                .filter { row -> row.get(EmotionMemoriesTable.userId) == userId }
                .count { row -> row.get(EmotionMemoriesTable.event) == event }
        }
    }

    override fun setLastEventTime(userId: String, event: String, time: java.time.LocalDateTime) {
        // No-op: last time can be derived from the most recent row for the event
    }

    override fun getLastEventTime(userId: String, event: String): java.time.LocalDateTime? {
        return transaction {
            EmotionMemoriesTable.selectAll()
                .asSequence()
                .filter { row -> row.get(EmotionMemoriesTable.userId) == userId }
                .sortedByDescending { row -> row.get(EmotionMemoriesTable.id) }
                .mapNotNull { row ->
                    if (row.get(EmotionMemoriesTable.event) == event) {
                        java.time.LocalDateTime.parse(row.get(EmotionMemoriesTable.timestamp))
                    } else {
                        null
                    }
                }
                .firstOrNull()
        }
    }
}
