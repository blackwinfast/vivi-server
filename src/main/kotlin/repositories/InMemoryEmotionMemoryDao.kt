package com.example.repositories

import com.example.models.EmotionMemoryEntry
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class InMemoryEmotionMemoryDao : EmotionMemoryDao {
    private val userMemories: MutableMap<String, MutableList<EmotionMemoryEntry>> = ConcurrentHashMap()
    private val eventCounts: MutableMap<String, MutableMap<String, Int>> = ConcurrentHashMap()
    private val lastEventTimes: MutableMap<String, MutableMap<String, LocalDateTime>> = ConcurrentHashMap()

    override fun addEntry(userId: String, entry: EmotionMemoryEntry) {
        val list = userMemories.computeIfAbsent(userId) { Collections.synchronizedList(ArrayList()) }
        list.add(entry)
        // cap at 100
        if (list.size > 100) synchronized(list) { if (list.size > 100) list.removeAt(0) }
    }

    override fun listEntries(userId: String): List<EmotionMemoryEntry> {
        val list = userMemories.getOrDefault(userId, Collections.synchronizedList(ArrayList()))
        return synchronized(list) { list.toList() }
    }

    override fun incrementEventCount(userId: String, event: String) {
        val counts = eventCounts.computeIfAbsent(userId) { ConcurrentHashMap() }
        counts.merge(event, 1) { old, one -> old + one }
    }

    override fun getEventCount(userId: String, event: String): Int {
        return eventCounts[userId]?.get(event) ?: 0
    }

    override fun setLastEventTime(userId: String, event: String, time: LocalDateTime) {
        val times = lastEventTimes.computeIfAbsent(userId) { ConcurrentHashMap() }
        times[event] = time
    }

    override fun getLastEventTime(userId: String, event: String): LocalDateTime? {
        return lastEventTimes[userId]?.get(event)
    }
}
