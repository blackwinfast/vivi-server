package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val user_id: String,
    val input_text: String,
)

@Serializable
data class ChatResponse(
    val response_text: String,
    val response_type: String = "CHAT_MESSAGE",
    val remaining_quota: Int? = null,
)

enum class EmotionType {
    SAD,
    NEUTRAL,
    ANGRY,
    HAPPY,
    EMERGENCY, // 极端负面内容，需要紧急处理
}

enum class UserTier {
    FREE,
    PREMIUM,
    VIP,
}

enum class ResponseType {
    CHAT_MESSAGE,
    EMERGENCY_RESOURCE,
}

enum class EventType {
    PRAISE,
    APOLOGY,
    CRITICISM,
    IGNORE,
    NEED,
    REPEATED,
    LONG_ABSENT,
}

enum class AIModel(val openRouterModelName: String, val displayName: String) {
    GPT_4O("openai/gpt-4o", "GPT-4o"), // 高质量模型
    GPT_3_5_TURBO("openai/gpt-3.5-turbo", "GPT-3.5 Turbo"), // 低成本模型
    CLAUDE_3_OPUS("anthropic/claude-3-opus", "Claude 3 Opus"), // 最高质量模型
    CLAUDE_3_SONNET("anthropic/claude-3-sonnet", "Claude 3 Sonnet"), // 高质量模型
    ;

    companion object {
        fun fromString(name: String): AIModel? {
            return values().find { it.name == name || it.openRouterModelName == name }
        }
    }
}
