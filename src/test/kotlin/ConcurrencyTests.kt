package com.example

import com.example.models.EmotionMemory
import com.example.services.AnniversaryService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrencyTests {

    @Test
    fun testAnniversaryConcurrentAdds() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "concurrent_user"
        val threads = 8
        val perThread = 200
        val latch = CountDownLatch(threads)
        val exec = Executors.newFixedThreadPool(threads)

        repeat(threads) { ti ->
            exec.submit {
                try {
                    repeat(perThread) { i ->
                        val title = "T-$ti-$i"
                        svc.addAnniversary(userId, title, LocalDate.of(2000, 1, 1).plusDays((i % 365).toLong()), com.example.services.CreatorType.USER, recurring = true)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        exec.shutdown()

        val all = svc.getUserAnniversaries(userId)
        assertEquals(threads * perThread, all.size, "Expected all concurrent adds to be present; got ${all.size}")
    }

    @Test
    fun testEmotionMemoryConcurrentRecord() {
        val mem = EmotionMemory(com.example.repositories.InMemoryEmotionMemoryDao())
        val userId = "mem_user"
        val threads = 6
        val perThread = 150
        val latch = CountDownLatch(threads)
        val exec = Executors.newFixedThreadPool(threads)
        val now = LocalDateTime.of(2025, 11, 10, 12, 0)

        repeat(threads) { ti ->
            exec.submit {
                try {
                    repeat(perThread) { i ->
                        val event = "E-$ti-$i"
                        mem.recordEvent(userId, event, com.example.models.EmotionType.HAPPY, 0.1, com.example.models.EventType.PRAISE, now.plusSeconds(i.toLong()))
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        exec.shutdown()

        val all = mem.getUserMemories(userId)
        // EmotionMemory caps per-user memories to 100 most recent entries; ensure we have the expected cap
        val expected = kotlin.math.min(threads * perThread, 100)
        assertEquals(expected, all.size, "Expected memory records (capped) to be $expected; got ${all.size}")
    }
}
