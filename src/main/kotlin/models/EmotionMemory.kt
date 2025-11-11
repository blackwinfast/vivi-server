package com.example.models

import com.example.repositories.EmotionMemoryDao
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * 情绪记忆条目
 * 记录事件、情绪强度、时间戳
 */
@Serializable
data class EmotionMemoryEntry(
    val event: String,
    val emotionType: EmotionType,
    val intensity: Double,
    @Serializable(with = LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime,
    val eventType: EventType,
)

/**
 * LocalDateTime 序列化器（简单实现，使用字符串）
 */
object LocalDateTimeSerializer : kotlinx.serialization.KSerializer<LocalDateTime> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDateTime", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

/**
 * EmotionMemory now delegates storage to an EmotionMemoryDao so we can swap in DB-backed storage later.
 */
class EmotionMemory(private val dao: EmotionMemoryDao) {

    fun recordEvent(
        userId: String,
        event: String,
        emotionType: EmotionType,
        intensity: Double,
        eventType: EventType = EventType.PRAISE,
        timestamp: LocalDateTime = LocalDateTime.now(),
    ) {
        val entry = EmotionMemoryEntry(event, emotionType, intensity, timestamp, eventType)
        dao.addEntry(userId, entry)
        dao.incrementEventCount(userId, event)
        dao.setLastEventTime(userId, event, timestamp)
    }

    fun getUserMemories(userId: String): List<EmotionMemoryEntry> = dao.listEntries(userId)

    fun getEventRepeatCount(userId: String, event: String): Int = dao.getEventCount(userId, event)

    fun isEventLongAbsent(userId: String, event: String, thresholdDays: Long = 7): Boolean {
        val last = dao.getLastEventTime(userId, event) ?: return true
        val daysPassed = java.time.Duration.between(last, LocalDateTime.now()).toDays()
        return daysPassed >= thresholdDays
    }

    fun replayEmotion(userId: String, eventKeyword: String): EmotionMemoryEntry? {
        return dao.listEntries(userId)
            .filter { it.event.contains(eventKeyword, ignoreCase = true) }
            .maxByOrNull { it.timestamp }
    }

    fun getRepairIntensity(userId: String, repairType: EventType): Double {
        return when (repairType) {
            EventType.APOLOGY -> -0.3
            EventType.PRAISE -> 0.2
            EventType.NEED -> 0.15
            else -> 0.0
        }
    }
}
