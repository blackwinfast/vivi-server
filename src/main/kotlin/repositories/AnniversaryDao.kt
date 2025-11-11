package com.example.repositories

import com.example.services.Anniversary

interface AnniversaryDao {
    fun save(anniversary: Anniversary)
    fun listByUser(userId: String): List<Anniversary>
    fun findById(id: String): Anniversary?
    fun update(anniversary: Anniversary): Boolean
    fun delete(id: String): Boolean
}
