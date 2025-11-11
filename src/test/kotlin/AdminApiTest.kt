package com.example

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminApiTest {

    @Test
    fun testAdminCrudEndpoints() = testApplication {
        application { module() }

        // create
        val createRes = client.post("/anniversaries/add") {
            header(io.ktor.http.HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {"user_id":"api_user","title":"APIAnn","date":"2025-11-10","createdBy":"AI","description":"from api","recurring":true}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, createRes.status)
        val json = createRes.bodyAsText()
        assertTrue(json.contains("ok"))

        // list
        val listRes = client.get("/admin/anniversaries/list?user_id=api_user")
        assertEquals(HttpStatusCode.OK, listRes.status)
        val listText = listRes.bodyAsText()
        println("DEBUG listText: $listText")
        println("DEBUG contains APIAnn? ${listText.contains("APIAnn")} ")
        assertTrue(listText.contains("APIAnn"), "listText did not contain APIAnn: $listText")

        // extract id
        val idRegex = "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"".toRegex()
        val match = idRegex.find(listText) ?: throw AssertionError("id not found in $listText")
        val id = match.groupValues[1]

        // get by id
        val getRes = client.get("/admin/anniversaries/$id")
        assertEquals(HttpStatusCode.OK, getRes.status)
        val getText = getRes.bodyAsText()
        println("DEBUG getText: $getText")
        assertTrue(getText.contains("APIAnn"), "getText did not contain APIAnn: $getText")

        // update
        val updateRes = client.put("/admin/anniversaries/$id") {
            header(io.ktor.http.HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"title\":\"APIAnnUpdated\"}")
        }
        assertEquals(HttpStatusCode.OK, updateRes.status)
        assertTrue(updateRes.bodyAsText().contains("ok"))

        val getAfter = client.get("/admin/anniversaries/$id")
        assertTrue(getAfter.bodyAsText().contains("APIAnnUpdated"))

        // delete
        val delRes = client.delete("/admin/anniversaries/$id")
        assertEquals(HttpStatusCode.OK, delRes.status)
        assertTrue(delRes.bodyAsText().contains("ok"))

        // ensure gone
        val listAfter = client.get("/admin/anniversaries/list?user_id=api_user")
        assertFalse(listAfter.bodyAsText().contains("APIAnnUpdated"))
    }
}
