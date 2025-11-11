package com.example.services

import com.example.models.UserTier
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户服务
 * 管理用户信息和会员等级
 * 实际生产环境应该连接数据库
 */
class UserService {

    // 用户会员等级存储（实际应该使用数据库）
    private val userTiers = ConcurrentHashMap<String, UserTier>()

    // 用户配额存储
    private val userQuotas = ConcurrentHashMap<String, Int>()

    init {
        // 初始化一些示例用户
        userTiers["user_01"] = UserTier.PREMIUM
        userQuotas["user_01"] = 100

        // 默认新用户为免费用户，配额为 10
    }

    /**
     * 获取用户会员等级
     * @param userId 用户ID
     * @return 会员等级，如果用户不存在则返回 FREE
     */
    fun getUserTier(userId: String): UserTier {
        return userTiers.getOrDefault(userId, UserTier.FREE)
    }

    /**
     * 获取用户剩余配额
     * @param userId 用户ID
     * @return 剩余配额
     */
    fun getRemainingQuota(userId: String): Int {
        return userQuotas.getOrDefault(userId, 10)
    }

    /**
     * 消费用户配额
     * @param userId 用户ID
     * @return 消费后的剩余配额，如果配额不足返回 -1
     */
    fun consumeQuota(userId: String): Int {
        val currentQuota = getRemainingQuota(userId)
        if (currentQuota <= 0) {
            return -1
        }
        val newQuota = currentQuota - 1
        userQuotas[userId] = newQuota
        return newQuota
    }

    /**
     * 设置用户会员等级
     * @param userId 用户ID
     * @param tier 会员等级
     */
    fun setUserTier(userId: String, tier: UserTier) {
        userTiers[userId] = tier
        // 根据会员等级设置初始配额
        when (tier) {
            UserTier.FREE -> userQuotas[userId] = 10
            UserTier.PREMIUM -> userQuotas[userId] = 100
            UserTier.VIP -> userQuotas[userId] = 1000
        }
    }
}
