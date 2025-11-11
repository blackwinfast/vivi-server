package com.example

import com.example.models.AIModel
import com.example.models.ChatRequest
import com.example.models.AIModel
import com.example.models.ChatRequest
import com.example.models.ChatResponse
import com.example.models.EmotionMemory
import com.example.models.EmotionType
import com.example.models.ResponseType
import com.example.repositories.DatabaseFactory
import com.example.repositories.InMemoryAnniversaryDao
import com.example.repositories.InMemoryEmotionMemoryDao
import com.example.repositories.SqliteAnniversaryDao
import com.example.repositories.SqliteEmotionMemoryDao
import com.example.services.AIModelService
import com.example.services.AnniversaryService
import com.example.services.AnniversaryUI
import com.example.services.CreatorType
import com.example.services.EmotionAnalysisService
import com.example.services.EmotionDisplay
import com.example.services.EmotionEngine
import com.example.services.ModelSelectionService
import com.example.services.PersonalityPromptBuilder
import com.example.services.UserEmotionService
import com.example.services.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
    // 初始化服务
    val emotionAnalysisService = EmotionAnalysisService()
    val userService = UserService()
    val modelSelectionService = ModelSelectionService()
    val aiModelService = AIModelService()

    // 微微靈魂架構服務
    // persistence DAOs - choose implementation based on config dao.mode (in-memory | sqlite)
    val daoMode = try {
        environment.config.propertyOrNull("dao.mode")?.getString() ?: environment.config.propertyOrNull("db.mode")?.getString() ?: "in-memory"
    } catch (e: Exception) { "in-memory" }

    // Allow opt-in verification of migrations/schema on startup (useful for staging).
    val verifyOnStartup = environment.config.propertyOrNull("dao.verifySchemaOnStartup")?.getString()?.toBoolean() ?: false
    DatabaseFactory.verifyOnConnect = verifyOnStartup

    val anniversaryDao = when (daoMode.lowercase()) {
        "sqlite" -> {
            val jdbc = environment.config.propertyOrNull("db.url")?.getString() ?: "jdbc:sqlite:data/dev.db"
            SqliteAnniversaryDao(jdbc)
        }
        else -> InMemoryAnniversaryDao()
    }

    val emotionMemoryDao = when (daoMode.lowercase()) {
        "sqlite" -> {
            val jdbc = environment.config.propertyOrNull("db.url")?.getString() ?: "jdbc:sqlite:data/dev.db"
            SqliteEmotionMemoryDao(jdbc)
        }
        else -> InMemoryEmotionMemoryDao()
    }

    val emotionMemory = com.example.models.EmotionMemory(emotionMemoryDao)
    val emotionEngine = EmotionEngine(emotionMemory)
    val userEmotionService = UserEmotionService()
    val personalityPromptBuilder = PersonalityPromptBuilder()
    val anniversaryService = com.example.services.AnniversaryService(debugEnabled = false, anniversaryDao = anniversaryDao)

    routing {
        get("/") {
            call.respondText("Chat API Server is running!")
        }

        // Replay emotion endpoint: 手動呼叫情緒重播（給前端或管理介面使用）
        post("/replay-emotion") {
            try {
                val body = call.receive<Map<String, String>>()
                val userId = body["user_id"] ?: run {
                    call.respond(ChatResponse(response_text = "错误：user_id 不能为空", response_type = ResponseType.CHAT_MESSAGE.name))
                    return@post
                }
                val keyword = body["event_keyword"] ?: body["keyword"] ?: ""

                if (keyword.isBlank()) {
                    call.respond(ChatResponse(response_text = "错误：event_keyword 不能为空", response_type = ResponseType.CHAT_MESSAGE.name))
                    return@post
                }

                val memoryEntry = emotionMemory.replayEmotion(userId, keyword)
                if (memoryEntry == null) {
                    call.respond(ChatResponse(response_text = "未找到匹配的情绪记忆", response_type = ResponseType.CHAT_MESSAGE.name))
                    return@post
                }

                val userEmotions = userEmotionService.getOrCreateUserEmotions(userId)
                val now = java.time.LocalDateTime.now()
                // 将记忆中的情绪强度应用为一次性情绪变化
                userEmotions.updateEmotion(memoryEntry.emotionType, memoryEntry.intensity, now = now)

                call.application.log.info("Replayed memory for user $userId: ${memoryEntry.event} -> ${memoryEntry.emotionType} +${memoryEntry.intensity}")

                call.respond(
                    ChatResponse(
                        response_text = "已重播记忆：${memoryEntry.event}，应用情绪：${memoryEntry.emotionType} 强度 ${memoryEntry.intensity}",
                        response_type = ResponseType.CHAT_MESSAGE.name,
                    ),
                )
            } catch (e: Exception) {
                call.application.log.error("Error in /replay-emotion", e)
                call.respond(
                    ChatResponse(
                        response_text = "处理重播请求时发生错误：${e.message}",
                        response_type = ResponseType.CHAT_MESSAGE.name,
                    ),
                )
            }
        }

        // Anniversary UI API (API-only): return message lists and upcoming summaries
        val anniversaryUI = AnniversaryUI(anniversaryService)

        get("/anniversaries/today") {
            val userId = call.request.queryParameters["user_id"] ?: run {
                call.respond(mapOf("error" to "user_id is required"))
                return@get
            }

            val messages = anniversaryUI.getTodayMessages(userId)
            call.respond(mapOf("messages" to messages))
        }

        get("/anniversaries/upcoming") {
            val userId = call.request.queryParameters["user_id"] ?: run {
                call.respond(mapOf("error" to "user_id is required"))
                return@get
            }
            val daysParam = call.request.queryParameters["days"]
            val days = daysParam?.toIntOrNull() ?: 7

            val summaries = anniversaryUI.getUpcomingSummaries(userId, days)
            call.respond(mapOf("upcoming" to summaries))
        }

        // 新增紀念日（方便測試）
        post("/anniversaries/add") {
            try {
                @kotlinx.serialization.Serializable
                data class AddAnniversaryRequest(
                    val user_id: String,
                    val title: String,
                    val date: String, // ISO date e.g. 2025-11-10
                    val createdBy: String, // USER or AI
                    val description: String? = null,
                    val recurring: Boolean = false,
                )

                val req = call.receive<AddAnniversaryRequest>()
                val createdBy = try {
                    CreatorType.valueOf(req.createdBy)
                } catch (e: Exception) {
                    call.respond(mapOf("error" to "invalid createdBy, must be USER or AI"))
                    return@post
                }

                val localDate = try {
                    java.time.LocalDate.parse(req.date)
                } catch (e: Exception) {
                    call.respond(mapOf("error" to "invalid date format, expected yyyy-MM-dd"))
                    return@post
                }

                val ann = anniversaryService.addAnniversary(
                    userId = req.user_id,
                    title = req.title,
                    date = localDate,
                    createdBy = createdBy,
                    description = req.description,
                    recurring = req.recurring,
                )

                @kotlinx.serialization.Serializable
                data class AdminAnnDto(val id: String, val title: String, val date: String, val createdBy: String, val description: String, val recurring: Boolean)

                @kotlinx.serialization.Serializable
                data class AddResp(val ok: Boolean, val anniversary: AdminAnnDto)

                val dto = AdminAnnDto(ann.id, ann.title, ann.date.toString(), ann.createdBy.name, ann.description ?: "", ann.recurring)
                call.respond(AddResp(true, dto))
            } catch (e: Exception) {
                call.application.log.error("Error in /anniversaries/add", e)
                call.respond(mapOf("error" to e.message))
            }
        }

        // Admin API: list all anniversaries for a user
        get("/admin/anniversaries/list") {
            val userId = call.request.queryParameters["user_id"] ?: run {
                call.respond(mapOf("error" to "user_id is required"))
                return@get
            }
            val list = anniversaryService.getUserAnniversaries(userId)

            @kotlinx.serialization.Serializable
            data class AdminAnnDto(val id: String, val title: String, val date: String, val createdBy: String, val description: String, val recurring: Boolean)
            val dtoList = list.map { ann -> AdminAnnDto(ann.id, ann.title, ann.date.toString(), ann.createdBy.name, ann.description ?: "", ann.recurring) }
            call.respond(dtoList)
        }

        // Admin API: get single anniversary by id
        get("/admin/anniversaries/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(mapOf("error" to "id required"))
                return@get
            }
            val ann = anniversaryService.getAnniversaryById(id) ?: run {
                call.respond(mapOf("error" to "not found"))
                return@get
            }

            @kotlinx.serialization.Serializable
            data class AdminAnnDto(val id: String, val title: String, val date: String, val createdBy: String, val description: String, val recurring: Boolean)
            val dto = AdminAnnDto(ann.id, ann.title, ann.date.toString(), ann.createdBy.name, ann.description ?: "", ann.recurring)
            call.respond(dto)
        }

        // Admin API: update anniversary
        put("/admin/anniversaries/{id}") {
            try {
                @kotlinx.serialization.Serializable
                data class UpdateReq(val title: String? = null, val date: String? = null, val description: String? = null, val recurring: Boolean? = null)

                val id = call.parameters["id"] ?: run {
                    call.respond(mapOf("error" to "id required"))
                    return@put
                }
                val req = call.receive<UpdateReq>()
                val date = req.date?.let { java.time.LocalDate.parse(it) }
                val updated = anniversaryService.updateAnniversary(id, title = req.title, date = date, description = req.description, recurring = req.recurring)
                call.respond(mapOf("ok" to updated))
            } catch (e: Exception) {
                call.application.log.error("Error updating anniversary", e)
                call.respond(mapOf("error" to e.message))
            }
        }

        // Admin API: delete anniversary
        delete("/admin/anniversaries/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(mapOf("error" to "id required"))
                return@delete
            }
            val deleted = anniversaryService.deleteAnniversary(id)
            call.respond(mapOf("ok" to deleted))
        }

        // Emotion display endpoint: return rendered emotion summary for front-end verification
        get("/emotion/display") {
            val userId = call.request.queryParameters["user_id"] ?: run {
                call.respond(mapOf("error" to "user_id is required"))
                return@get
            }

            val userEmotions = userEmotionService.getOrCreateUserEmotions(userId)
            val now = java.time.LocalDateTime.now()
            val emotionDisplay = EmotionDisplay()

            // 取得今天的紀念日（如果有），並一併傳入渲染以便 UI 顯示
            val todayAnniversaries = anniversaryService.getTodayAnniversaries(userId)
            val rendered = emotionDisplay.renderEmotion(userEmotions, userId, todayAnniversaries, now)
            call.respond(rendered)
        }

        // Admin UI (simple) to view anniversaries + emotion state
        get("/admin/anniversaries") {
            call.respondText(
                // language=html
                """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta charset="utf-8" />
                                    <title>Admin - Anniversaries & Emotions</title>
                                    <style>
                                        body { font-family: Arial, sans-serif; padding: 20px; }
                                        input, button { padding: 8px; margin: 4px; }
                                        .box { border: 1px solid #ddd; padding: 12px; margin-top: 10px; }
                                        pre { background:#f8f8f8; padding:10px; overflow:auto }
                                    </style>
                                </head>
                                <body>
                                    <h2>Admin: Anniversaries & Emotion Display</h2>
                                    <div>
                                        <label>User ID: <input id="userId" value="test_user"/></label>
                                        <button id="loadAll">Load All</button>
                                        <button id="createSample">Create Sample Anniversary</button>
                                    </div>

                                                        <div class="box">
                                                            <h3>Today's Anniversaries</h3>
                                                            <ul id="todayList"></ul>
                                                        </div>

                                                        <div class="box">
                                                            <h3>Upcoming Anniversaries</h3>
                                                            <pre id="upcomingPre">-</pre>
                                                        </div>

                                                        <div class="box">
                                                            <h3>Emotion Display</h3>
                                                            <div>
                                                                <strong>Primary:</strong> <span id="emotionPrimary">-</span>
                                                            </div>
                                                            <div>
                                                                <strong>Intensity:</strong> <span id="emotionIntensity">-</span>
                                                            </div>
                                                            <div>
                                                                <strong>Summary:</strong>
                                                                <div id="emotionSummary">-</div>
                                                            </div>
                                                            <div>
                                                                <strong>Tone:</strong> <span id="emotionTone">-</span>
                                                            </div>
                                                            <div>
                                                                <strong>All Emotions:</strong>
                                                                <ul id="emotionAll"></ul>
                                                            </div>
                                                        </div>

                                                        <div class="box">
                                                            <h3>Combined View</h3>
                                                            <div id="combinedView">-</div>
                                                        </div>

                                    <script>
                                        async function fetchJson(url, opts) {
                                            const r = await fetch(url, opts);
                                            return await r.json();
                                        }

                                        async function loadAll() {
                                            const userId = document.getElementById('userId').value;
                                            if (!userId) return alert('user_id required');

                                                                    // today's messages
                                                                    const today = await fetchJson(`/anniversaries/today?user_id=${'$'}{encodeURIComponent(userId)}`);
                                                                    const todayList = document.getElementById('todayList');
                                                                    todayList.innerHTML = '';
                                                                    (today.messages || []).forEach(m => {
                                                                        const li = document.createElement('li'); li.textContent = m; todayList.appendChild(li);
                                                                    });

                                                                    // upcoming
                                                                    const upcoming = await fetchJson(`/anniversaries/upcoming?user_id=${'$'}{encodeURIComponent(userId)}&days=30`);
                                                                    document.getElementById('upcomingPre').textContent = JSON.stringify(upcoming, null, 2);

                                                                    // emotion display (structured)
                                                                    const emotion = await fetchJson(`/emotion/display?user_id=${'$'}{encodeURIComponent(userId)}`);
                                                                    document.getElementById('emotionPrimary').textContent = emotion.primary || '-';
                                                                    document.getElementById('emotionIntensity').textContent = (emotion.intensity !== undefined) ? emotion.intensity : '-';
                                                                    document.getElementById('emotionSummary').textContent = emotion.summary || '-';
                                                                    document.getElementById('emotionTone').textContent = emotion.tone || '-';
                                                                    const emotionAll = document.getElementById('emotionAll');
                                                                    emotionAll.innerHTML = '';
                                                                    if (emotion.all) {
                                                                        Object.entries(emotion.all).forEach(([k,v]) => {
                                                                            const li = document.createElement('li'); li.textContent = `${'$'}{k}: ${'$'}{v}`; emotionAll.appendChild(li);
                                                                        });
                                                                    }

                                                                    // combined view: show anniversary messages + emotion summary in one panel
                                                                    const combined = document.getElementById('combinedView');
                                                                    const arrMsgs = today.messages || [];
                                                                    combined.innerHTML = '';
                                                                    const h = document.createElement('div');
                                                                    h.innerHTML = `<strong>微微狀態：</strong> ${'$'}{emotion.primary || '-'} (${'$'}{emotion.tone || '-'}) - ${'$'}{emotion.summary || '-'}<br/>`;
                                                                    combined.appendChild(h);
                                                                    if (arrMsgs.length > 0) {
                                                                        const am = document.createElement('div');
                                                                        am.innerHTML = `<strong>今日紀念日：</strong>`;
                                                                        const ul = document.createElement('ul');
                                                                        arrMsgs.forEach(m => { const li = document.createElement('li'); li.textContent = m; ul.appendChild(li); });
                                                                        combined.appendChild(am);
                                                                        combined.appendChild(ul);
                                                                    } else {
                                                                        const nm = document.createElement('div'); nm.textContent = '今日沒有紀念日'; combined.appendChild(nm);
                                                                    }
                                        }

                                        document.getElementById('loadAll').addEventListener('click', loadAll);

                                        document.getElementById('createSample').addEventListener('click', async () => {
                                            const userId = document.getElementById('userId').value;
                                            const today = new Date();
                                            const yyyy = today.getFullYear();
                                            const mm = String(today.getMonth()+1).padStart(2,'0');
                                            const dd = String(today.getDate()).padStart(2,'0');
                                            const body = {
                                                user_id: userId,
                                                title: '微微示範紀念日',
                                                date: `${'$'}{yyyy}-${'$'}{mm}-${'$'}{dd}`,
                                                createdBy: 'AI',
                                                description: '由 Admin UI 建立',
                                                recurring: true
                                            };
                                            const res = await fetch('/anniversaries/add', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body)});
                                            const json = await res.json();
                                            alert('Create result: ' + JSON.stringify(json));
                                            await loadAll();
                                        });

                                        // auto-load on open
                                        window.addEventListener('load', loadAll);
                                    </script>
                                </body>
                                </html>
                                """,
                contentType = io.ktor.http.ContentType.Text.Html,
            )
        }

        // Test endpoints removed: cleaning up residual test routes per request

        post("/chat") {
            try {
                // 接收请求
                val request = call.receive<ChatRequest>()

                call.application.log.info("Received chat request from user: ${request.user_id}, input length: ${request.input_text.length}")

                // 验证请求
                if (request.user_id.isBlank() || request.input_text.isBlank()) {
                    call.application.log.warn("Invalid request: empty user_id or input_text")
                    call.respond(
                        ChatResponse(
                            response_text = "错误：user_id 和 input_text 不能为空",
                            response_type = ResponseType.CHAT_MESSAGE.name,
                        ),
                    )
                    return@post
                }

                // 检查用户配额
                val remainingQuota = userService.getRemainingQuota(request.user_id)
                if (remainingQuota <= 0) {
                    call.application.log.warn("User ${request.user_id} quota exhausted")
                    call.respond(
                        ChatResponse(
                            response_text = "抱歉，您的使用次数已用完。请升级会员以获取更多配额。",
                            response_type = ResponseType.CHAT_MESSAGE.name,
                            remaining_quota = 0,
                        ),
                    )
                    return@post
                }

                // 1. 获取或创建用户情绪状态
                val userEmotions = userEmotionService.getOrCreateUserEmotions(request.user_id)
                val now = java.time.LocalDateTime.now()

                // 2. 应用时间衰减
                emotionEngine.applyTimeDecay(userEmotions, now)

                // 自动情绪重播检测（简单实现）：
                // 如果使用者輸入包含「上次」且包含引號「」或""，則嘗試提取引號內關鍵字並重播記憶
                var replayAppliedMessage: String? = null
                try {
                    val inputText = request.input_text
                    if (inputText.contains("上次") || inputText.contains("上次說")) {
                        // 優先尋找中文書名號或引號
                        val quoteKeyword = when {
                            inputText.contains('「') && inputText.contains('」') -> {
                                inputText.substringAfter('「').substringBefore('」').trim()
                            }
                            inputText.contains('"') -> {
                                inputText.substringAfter('"').substringBefore('"').trim()
                            }
                            else -> null
                        }

                        if (!quoteKeyword.isNullOrBlank()) {
                            val mem = emotionMemory.replayEmotion(request.user_id, quoteKeyword)
                            if (mem != null) {
                                // 將記憶套回使用者情緒狀態
                                userEmotions.updateEmotion(mem.emotionType, mem.intensity, now = now)
                                replayAppliedMessage = "已重播記憶：'${mem.event}'，套用情緒 ${mem.emotionType} 強度 ${mem.intensity}"
                                call.application.log.info("Replay applied for user=${request.user_id}, event=${mem.event}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    call.application.log.warn("Error during replay detection: ${e.message}")
                }

                // 3. 检查纪念日
                val todayAnniversaries = anniversaryService.getTodayAnniversaries(request.user_id)
                if (todayAnniversaries.isNotEmpty()) {
                    val anniversaryBoost = anniversaryService.getEmotionBoost(todayAnniversaries)
                    userEmotions.updateEmotion(EmotionType.HAPPY, anniversaryBoost, now = now)
                    call.application.log.info("Anniversary detected for user: ${request.user_id}, boost: $anniversaryBoost")
                }

                // 4. 情绪分析（检测用户输入的情绪）
                val detectedEmotion = emotionAnalysisService.analyzeEmotion(request.input_text)
                call.application.log.info("Detected emotion: $detectedEmotion for user: ${request.user_id}")

                // 5. 检查是否为紧急情况
                if (detectedEmotion == EmotionType.EMERGENCY) {
                    call.application.log.warn("Emergency situation detected for user: ${request.user_id}")
                    val emergencyResponse = ChatResponse(
                        response_text = "我注意到你现在的情绪非常低落。请记住，你并不孤单。如果你需要帮助，可以联系：\n\n" +
                            "• 生命线 1995（台湾）\n" +
                            "• 张老师专线 1980\n" +
                            "• 当地心理健康支持热线\n\n" +
                            "请相信，情况会好转的。",
                        response_type = ResponseType.EMERGENCY_RESOURCE.name,
                        remaining_quota = remainingQuota, // 紧急情况不消耗配额
                    )
                    call.respond(emergencyResponse)
                    return@post
                }

                // 6. 处理事件对微微情绪的影响
                val intensityChanges = emotionEngine.processEvent(
                    userId = request.user_id,
                    userEmotions = userEmotions,
                    event = request.input_text,
                    detectedEmotion = detectedEmotion,
                    now = now,
                )
                call.application.log.info("Emotion changes: $intensityChanges for user: ${request.user_id}")

                // 7. 获取微微当前的主要情绪（用于模型选择）
                val weiweiPrimaryEmotion = userEmotions.getPrimaryEmotion(now) ?: EmotionType.NEUTRAL
                call.application.log.info("Weiwei primary emotion: $weiweiPrimaryEmotion")

                // 8. 获取用户会员等级
                val userTier = userService.getUserTier(request.user_id)
                call.application.log.info("User tier: $userTier for user: ${request.user_id}")

                // 9. 动态选择模型（基于用户情绪，因为我们要回应用户的情绪）
                val selectedModel = modelSelectionService.selectModel(detectedEmotion, userTier)
                call.application.log.info("Selected model: ${selectedModel.displayName} (${selectedModel.openRouterModelName}) for emotion: $detectedEmotion, tier: $userTier")

                // 10. 构建个性提示词（基于微微的情绪状态）
                val personalityPrompt = personalityPromptBuilder.buildPersonalityPrompt(
                    userEmotions = userEmotions,
                    isHighQuality = selectedModel == AIModel.CLAUDE_3_OPUS || selectedModel == AIModel.CLAUDE_3_SONNET || selectedModel == AIModel.GPT_4O,
                )

                // 11. 如果有纪念日，添加到提示词
                val finalPrompt = if (todayAnniversaries.isNotEmpty()) {
                    val anniversaryMessage = anniversaryService.generateAnniversaryMessage(todayAnniversaries)
                    "$personalityPrompt\n\n特别提醒：$anniversaryMessage 今天微微的心情特别好！"
                } else {
                    personalityPrompt
                }

                // 12. 生成 AI 回复
                val aiResponse = aiModelService.generateResponse(
                    model = selectedModel,
                    userInput = request.input_text,
                    emotion = detectedEmotion,
                    personalityPrompt = finalPrompt,
                )
                call.application.log.info("AI response generated, length: ${aiResponse.length}")

                // 6. 消费配额
                val newQuota = userService.consumeQuota(request.user_id)
                call.application.log.info("Quota consumed for user: ${request.user_id}, remaining: $newQuota")

                // 7. 构建并返回响应（确保 JSON 格式正确）
                val response = ChatResponse(
                    response_text = aiResponse,
                    response_type = ResponseType.CHAT_MESSAGE.name,
                    remaining_quota = newQuota,
                )

                call.respond(response)
            } catch (e: Exception) {
                call.application.log.error("Error processing chat request", e)
                call.respond(
                    ChatResponse(
                        response_text = "抱歉，处理您的请求时发生错误。请稍后再试。",
                        response_type = ResponseType.CHAT_MESSAGE.name,
                    ),
                )
            }
        }
    }
