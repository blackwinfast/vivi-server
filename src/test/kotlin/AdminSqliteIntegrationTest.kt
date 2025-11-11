package com.example

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminSqliteIntegrationTest {

    @Test
    fun testAdminCrudPersistenceAcrossRestarts() {
        // prepare a file-backed sqlite db under build/tmp
        val dbDir = Paths.get("build", "tmp")
        Files.createDirectories(dbDir)
        val dbFile = dbDir.resolve("admin_integ.db").toAbsolutePath().toString()
        val jdbc = "jdbc:sqlite:$dbFile"

        // First app instance: create an anniversary
        testApplication {
            // supply config to enable sqlite mode and db url
            environment { config = MapApplicationConfig("dao.mode" to "sqlite", "db.url" to jdbc) }
            application { module() }

            val createRes = client.post("/anniversaries/add") {
                header(io.ktor.http.HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
                    {"user_id":"persist_user","title":"PersistAnn","date":"2025-11-11","createdBy":"AI","description":"persist test","recurring":false}
                    """.trimIndent(),
                )
            }
            assertEquals(HttpStatusCode.OK, createRes.status)
            val listRes = client.get("/admin/anniversaries/list?user_id=persist_user")
            assertEquals(HttpStatusCode.OK, listRes.status)
            val listText = listRes.bodyAsText()
            assertTrue(listText.contains("PersistAnn"), "Expected created anniversary present: $listText")

            // extract id
            val idRegex = "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"".toRegex()
            val match = idRegex.find(listText) ?: throw AssertionError("id not found in $listText")
            val id = match.groupValues[1]
            assertNotNull(id)
        }

        // Second app instance (new Application) pointing to same DB: ensure anniversary still present
        testApplication {
            environment { config = MapApplicationConfig("dao.mode" to "sqlite", "db.url" to jdbc) }
            application { module() }

            val listRes2 = client.get("/admin/anniversaries/list?user_id=persist_user")
            assertEquals(HttpStatusCode.OK, listRes2.status)
            val text2 = listRes2.bodyAsText()
            assertTrue(text2.contains("PersistAnn"), "Expected persisted anniversary present after restart: $text2")

            // cleanup: delete all found entries for this user
            val idRegex = "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"".toRegex()
            idRegex.findAll(text2).forEach { m ->
                val id = m.groupValues[1]
                val del = client.delete("/admin/anniversaries/$id")
                assertEquals(HttpStatusCode.OK, del.status)
            }
        }

        // ensure DB file exists (persistence)
        val exists = Files.exists(Paths.get(dbFile))
        assertTrue(exists, "Expected DB file to exist at $dbFile")
    }
}
