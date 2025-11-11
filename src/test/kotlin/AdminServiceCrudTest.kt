package com.example

import com.example.services.AnniversaryService
import com.example.services.CreatorType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminServiceCrudTest {

    @Test
    fun testServiceCreateUpdateDelete() {
        val svc = AnniversaryService(anniversaryDao = com.example.repositories.InMemoryAnniversaryDao())
        val userId = "admin_user"
        val ann = svc.addAnniversary(userId, "Xmas", LocalDate.of(2000, 12, 25), CreatorType.USER, recurring = true)
        assertTrue(svc.getUserAnniversaries(userId).any { it.id == ann.id })

        // update title
        val updated = svc.updateAnniversary(ann.id, title = "Xmas Edited")
        assertTrue(updated)
        val fetched = svc.getAnniversaryById(ann.id)
        assertEquals("Xmas Edited", fetched?.title)

        // delete
        val deleted = svc.deleteAnniversary(ann.id)
        assertTrue(deleted)
        assertFalse(svc.getUserAnniversaries(userId).any { it.id == ann.id })
    }
}
