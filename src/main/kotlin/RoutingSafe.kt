
@file:Suppress(
    "LongMethod",
    "NoUnusedImports",
    "UnusedImport",
    "CyclomaticComplexMethod",
    "Indentation",
)

package com.example

import com.example.models.ChatResponse
import com.example.models.ResponseType
import com.example.repositories.DatabaseFactory
import com.example.repositories.InMemoryAnniversaryDao
import com.example.repositories.InMemoryEmotionMemoryDao
import com.example.repositories.SqliteAnniversaryDao
import com.example.repositories.SqliteEmotionMemoryDao
import com.example.services.AnniversaryService
import com.example.services.CreatorType
import com.example.services.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Temporary, minimal routing used while the large Routing.kt is excluded.
 * Keeps the project compilable and provides tiny endpoints used by tests.
 */
@Suppress("CyclomaticComplexMethod")
fun Application.configureRouting() {
    val userService = UserService()

    val daoMode = try {
        environment.config.propertyOrNull("dao.mode")?.getString() ?: "in-memory"
    } catch (e: Exception) {
        "in-memory"
    }

    DatabaseFactory.verifyOnConnect = environment.config
        .propertyOrNull("dao.verifySchemaOnStartup")
        ?.getString()
        ?.toBoolean()
        ?: false

    val anniversaryDao = if (daoMode == "sqlite") {
        SqliteAnniversaryDao(
            environment.config.propertyOrNull("db.url")?.getString()
                ?: "jdbc:sqlite:data/dev.db",
        )
    } else {
        InMemoryAnniversaryDao()
    }

    val emotionMemoryDao = if (daoMode == "sqlite") {
        SqliteEmotionMemoryDao(
            environment.config.propertyOrNull("db.url")?.getString()
                ?: "jdbc:sqlite:data/dev.db",
        )
    } else {
        InMemoryEmotionMemoryDao()
    }

    val anniversaryService = AnniversaryService(
        debugEnabled = false,
        anniversaryDao = anniversaryDao,
    )

    routing {
        get("/") {
            call.respondText("Chat API Server (minimal) is running!")
        }

        post("/chat") {
            try {
                val parsed = Json.parseToJsonElement(call.receiveText()).jsonObject
                val userId = parsed["user_id"]?.jsonPrimitive?.content ?: ""
                val input = parsed["input_text"]?.jsonPrimitive?.content ?: ""
                if (userId.isBlank() || input.isBlank()) {
                    call.respond(
                        ChatResponse(
                            response_text = "Invalid request",
                            response_type = ResponseType.CHAT_MESSAGE.name,
                        ),
                    )
                    return@post
                }

                val remaining = userService.getRemainingQuota(userId)
                call.respond(
                    ChatResponse(
                        response_text = "ok",
                        response_type = ResponseType.CHAT_MESSAGE.name,
                        remaining_quota = remaining - 1,
                    ),
                )
            } catch (e: Exception) {
                call.application.log.error("/chat error", e)
                call.respond(
                    ChatResponse(
                        response_text = "error: ${e.message}",
                        response_type = ResponseType.CHAT_MESSAGE.name,
                    ),
                )
            }
        }

        post("/anniversaries/add") {
            try {
                val bodyText = call.receiveText()
                val json = Json.parseToJsonElement(bodyText).jsonObject
                val userId = json["user_id"]?.jsonPrimitive?.content ?: run {
                    call.respond(mapOf("error" to "user_id required"))
                    return@post
                }
                val title = json["title"]?.jsonPrimitive?.content ?: ""
                val dateStr = json["date"]?.jsonPrimitive?.content ?: "1970-01-01"
                val createdByStr = json["createdBy"]?.jsonPrimitive?.content ?: "USER"
                val description = json["description"]?.jsonPrimitive?.content
                val recurring = json["recurring"]?.jsonPrimitive?.content?.toBoolean() ?: false

                val ann = anniversaryService.addAnniversary(
                    userId = userId,
                    title = title,
                    date = java.time.LocalDate.parse(dateStr),
                    createdBy = CreatorType.valueOf(createdByStr),
                    description = description,
                    recurring = recurring,
                )
                call.respond(mapOf("status" to "ok", "id" to ann.id))
            } catch (e: Exception) {
                call.application.log.error("/anniversaries/add error", e)
                call.respond(mapOf("error" to e.message))
            }
        }

        get("/admin/anniversaries/list") {
            val userId = call.request.queryParameters["user_id"] ?: run {
                call.respond(mapOf("error" to "user_id is required"))
                return@get
            }
            val list = anniversaryService.getUserAnniversaries(userId)
            call.respond(list)
        }

        get("/admin/anniversaries/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(mapOf("error" to "id required"))
                return@get
            }
            val ann = anniversaryService.getAnniversaryById(id)
            if (ann == null) call.respond(mapOf("error" to "not found")) else call.respond(ann)
        }

        put("/admin/anniversaries/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(mapOf("error" to "id required"))
                return@put
            }
            val bodyText = call.receiveText()
            val json = Json.parseToJsonElement(bodyText).jsonObject
            val title = json["title"]?.jsonPrimitive?.content
            val updated = anniversaryService.updateAnniversary(id, title = title)
            if (updated) call.respond(mapOf("status" to "ok")) else call.respond(mapOf("error" to "not updated"))
        }

        delete("/admin/anniversaries/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(mapOf("error" to "id required"))
                return@delete
            }
            val deleted = anniversaryService.deleteAnniversary(id)
            if (deleted) call.respond(mapOf("status" to "ok")) else call.respond(mapOf("error" to "not deleted"))
        }
    }
}
