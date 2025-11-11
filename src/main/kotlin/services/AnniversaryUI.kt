package com.example.services

/**
 * Anniversary UI helper (API-only)
 * 提供給前端的簡單介面，將 AnniversaryService 的結果轉成要顯示的訊息或簡化資料
 */
class AnniversaryUI(private val anniversaryService: AnniversaryService) {

    /**
     * 取得今天的紀念日訊息（簡短文字列表）
     */
    fun getTodayMessages(userId: String, today: java.time.LocalDate? = null): List<String> {
        val list = if (today != null) anniversaryService.getTodayAnniversaries(userId, today) else anniversaryService.getTodayAnniversaries(userId)
        if (list.isEmpty()) return emptyList()

        return list.map { ann ->
            val creator = if (ann.createdBy == CreatorType.AI) "我（微微）" else "你"
            val desc = ann.description?.let { "（$it）" } ?: ""
            "今天是${creator}设定的「${ann.title}」$desc"
        }
    }

    /**
     * 取得未來幾天內的紀念日摘要（結構化資料，供 UI 顯示）
     */
    fun getUpcomingSummaries(userId: String, days: Int = 7, today: java.time.LocalDate? = null): List<Map<String, Any>> {
        val upcoming = if (today != null) anniversaryService.getUpcomingAnniversaries(userId, days, today) else anniversaryService.getUpcomingAnniversaries(userId, days)
        return upcoming.map { ann ->
            mapOf(
                "id" to ann.id,
                "title" to ann.title,
                "date" to ann.date.toString(),
                "createdBy" to ann.createdBy.name,
                "description" to (ann.description ?: ""),
                "recurring" to ann.recurring,
                "message" to if (ann.recurring) "每年重复的纪念日" else "特定年份纪念日",
            )
        }
    }
}
