package com.example.repositories

import com.example.models.EmotionMemoryEntry
import java.time.LocalDateTime

interface EmotionMemoryDao {
    fun addEntry(userId: String, entry: EmotionMemoryEntry)
    fun listEntries(userId: String): List<EmotionMemoryEntry>
    fun incrementEventCount(userId: String, event: String)
    fun getEventCount(userId: String, event: String): Int
    fun setLastEventTime(userId: String, event: String, time: LocalDateTime)
    fun getLastEventTime(userId: String, event: String): LocalDateTime?
}
