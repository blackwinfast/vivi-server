package com.example.services

import com.example.models.AIModel
import com.example.models.EmotionType
import com.example.models.UserTier

/**
 * 模型选择服务
 * 根据情绪分析结果和用户会员等级动态选择 AI 模型
 */
class ModelSelectionService {

    /**
     * 根据情绪和用户等级选择模型
     * @param emotion 情绪类型
     * @param userTier 用户会员等级
     * @return 选择的 AI 模型
     */
    fun selectModel(emotion: EmotionType, userTier: UserTier): AIModel {
        // 紧急情况：无论用户等级，都使用最高质量模型
        if (emotion == EmotionType.EMERGENCY) {
            return AIModel.CLAUDE_3_OPUS
        }

        // 悲伤情绪：使用高质量模型，给予更好的情感支持
        if (emotion == EmotionType.SAD) {
            return when (userTier) {
                UserTier.VIP -> AIModel.CLAUDE_3_OPUS // VIP 使用最高质量模型
                UserTier.PREMIUM -> AIModel.CLAUDE_3_SONNET // 高级会员使用高质量模型
                UserTier.FREE -> AIModel.GPT_4O // 即使是免费用户，悲伤时也使用高质量模型
            }
        }

        // 愤怒情绪：使用高质量模型，帮助用户冷静分析
        if (emotion == EmotionType.ANGRY) {
            return when (userTier) {
                UserTier.VIP -> AIModel.CLAUDE_3_SONNET
                UserTier.PREMIUM -> AIModel.GPT_4O
                UserTier.FREE -> AIModel.GPT_4O // 免费用户也使用高质量模型
            }
        }

        // 其他情绪（快乐、中性）：根据用户等级选择
        return when (userTier) {
            UserTier.VIP -> AIModel.CLAUDE_3_SONNET // VIP 使用高质量模型
            UserTier.PREMIUM -> AIModel.GPT_4O // 高级会员使用中等质量模型
            UserTier.FREE -> AIModel.GPT_3_5_TURBO // 免费用户使用低成本模型
        }
    }
}
