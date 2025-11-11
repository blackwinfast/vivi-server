package com.example.services

import com.example.repositories.AnniversaryDao
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * LocalDate 序列化器
 */
object LocalDateSerializer : kotlinx.serialization.KSerializer<LocalDate> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDate", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/**
 * 纪念日数据类
 */
@Serializable
data class Anniversary(
    val id: String,
    val userId: String,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val createdBy: CreatorType,
    val description: String? = null,
    val recurring: Boolean = false,
)

/**
 * 创建者类型
 */
enum class CreatorType {
    USER, // 用户创建
    AI, // AI（微微）创建
}

/**
 * 纪念日服务
 * 管理纪念日，到期自动提醒，提升高兴情绪
 */
class AnniversaryService(
    val debugEnabled: Boolean = false,
    private val anniversaryDao: AnniversaryDao,
) {

    // human-readable debug output of the last upcoming calculation (for tests)
    var lastDebug: String? = null

    // storage delegated to AnniversaryDao

    init {
        // 可以在这里初始化一些默认纪念日
    }

    /**
     * 添加纪念日
     */
    fun addAnniversary(
        userId: String,
        title: String,
        date: LocalDate,
        createdBy: CreatorType,
        description: String? = null,
        recurring: Boolean = false,
    ): Anniversary {
        val anniversary = Anniversary(
            // use UUID to ensure unique ids even when created in the same millisecond
            id = "${userId}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}",
            userId = userId,
            title = title,
            date = date,
            createdBy = createdBy,
            description = description,
            recurring = recurring,
        )

        anniversaryDao.save(anniversary)
        return anniversary
    }

    /**
     * 获取用户的所有纪念日
     */
    fun getUserAnniversaries(userId: String): List<Anniversary> {
        return anniversaryDao.listByUser(userId)
    }

    // Delegate CRUD operations to DAO (implemented below)

    /**
     * 检查今天是否有纪念日
     * @return 今天的纪念日列表
     */
    fun getTodayAnniversaries(userId: String, today: LocalDate = LocalDate.now()): List<Anniversary> {
        val anniversaries = getUserAnniversaries(userId)
        return anniversaries.filter { anniversary ->
            // 检查是否匹配今天的日期
            val matchesToday = anniversary.date.month == today.month &&
                anniversary.date.dayOfMonth == today.dayOfMonth

            if (matchesToday && anniversary.recurring) {
                true // 重复纪念日，每年都提醒
            } else if (matchesToday && anniversary.date.year == today.year) {
                true // 非重复纪念日，只匹配具体年份
            } else {
                false
            }
        }
    }

    /**
     * 检查即将到来的纪念日（未来7天内）
     */
    fun getUpcomingAnniversaries(userId: String, days: Int = 7, today: LocalDate = LocalDate.now()): List<Anniversary> {
        val anniversaries = getUserAnniversaries(userId)

        // Compute next occurrence per anniversary and include those within [0, days)
        val debugLines = mutableListOf<String>()
        val picked = anniversaries.mapNotNull { ann ->
            val nextDate = if (ann.recurring) {
                var candidate = ann.date.withYear(today.year)
                if (candidate.isBefore(today)) candidate = candidate.plusYears(1)
                candidate
            } else {
                if (ann.date.isBefore(today)) {
                    debugLines.add("skipping non-recurring past: ${ann.title} (${ann.date})")
                    return@mapNotNull null
                }
                ann.date
            }

            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextDate)
            debugLines.add("ann=${ann.title}, original=${ann.date}, next=$nextDate, daysUntil=$daysUntil")
            if (daysUntil >= 0 && daysUntil < days) ann else null
        }.distinctBy { it.id }

        if (debugEnabled) {
            val sb = StringBuilder()
            sb.append("getUpcomingAnniversaries debug for user=$userId, today=$today, days=$days\n")
            sb.append("all=${anniversaries}\n")
            debugLines.forEach { sb.append(it).append("\n") }
            sb.append("picked=${picked}\n")
            lastDebug = sb.toString()
            println(lastDebug)
        }

        return picked
    }

    /**
     * 获取纪念日对情绪的影响
     * 纪念日会提升高兴情绪
     */
    fun getEmotionBoost(anniversaries: List<Anniversary>): Double {
        if (anniversaries.isEmpty()) return 0.0

        // 每个纪念日提升0.3的高兴情绪
        return anniversaries.size * 0.3
    }

    /**
     * 生成纪念日提醒消息
     */
    fun generateAnniversaryMessage(anniversaries: List<Anniversary>): String? {
        if (anniversaries.isEmpty()) return null

        val messages = anniversaries.map { anniversary ->
            val creator = if (anniversary.createdBy == CreatorType.AI) "我" else "你"
            val desc = if (anniversary.description != null) "(${anniversary.description})" else ""
            "今天是${creator}设定的「${anniversary.title}」$desc"
        }

        return messages.joinToString("，") + "！"
    }

    // DAO-backed CRUD helpers
    fun getAnniversaryById(id: String): Anniversary? = anniversaryDao.findById(id)

    fun updateAnniversary(id: String, title: String? = null, date: LocalDate? = null, description: String? = null, recurring: Boolean? = null): Boolean {
        val existing = anniversaryDao.findById(id) ?: return false
        val updated = existing.copy(
            title = title ?: existing.title,
            date = date ?: existing.date,
            description = description ?: existing.description,
            recurring = recurring ?: existing.recurring,
        )
        return anniversaryDao.update(updated)
    }

    fun deleteAnniversary(id: String): Boolean = anniversaryDao.delete(id)
}
