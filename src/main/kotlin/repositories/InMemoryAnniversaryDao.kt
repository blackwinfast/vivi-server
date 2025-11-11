package com.example.repositories

import com.example.services.Anniversary
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class InMemoryAnniversaryDao : AnniversaryDao {
    private val map: MutableMap<String, MutableList<Anniversary>> = ConcurrentHashMap()

    override fun save(anniversary: Anniversary) {
        val list = map.computeIfAbsent(anniversary.userId) { Collections.synchronizedList(ArrayList()) }
        list.add(anniversary)
    }

    override fun listByUser(userId: String): List<Anniversary> {
        val list = map.getOrDefault(userId, Collections.synchronizedList(ArrayList()))
        return synchronized(list) { list.toList() }
    }

    override fun findById(id: String): Anniversary? {
        map.values.forEach { list ->
            synchronized(list) {
                list.forEach { if (it.id == id) return it }
            }
        }
        return null
    }

    override fun update(anniversary: Anniversary): Boolean {
        map.values.forEach { list ->
            synchronized(list) {
                val idx = list.indexOfFirst { it.id == anniversary.id }
                if (idx >= 0) {
                    list[idx] = anniversary
                    return true
                }
            }
        }
        return false
    }

    override fun delete(id: String): Boolean {
        map.values.forEach { list ->
            synchronized(list) {
                val removed = list.removeIf { it.id == id }
                if (removed) return true
            }
        }
        return false
    }
}
