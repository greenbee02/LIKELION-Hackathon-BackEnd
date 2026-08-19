package com.cju.likelion.cardcollection.reward.service;

import java.math.BigDecimal;

/**
 * 공식 컬렉션 달성률과 collection_rewards.required_percentage를 비교한다.
 */
public final class RewardEligibilityEvaluator {

    private RewardEligibilityEvaluator() {
    }

    public static boolean isEligible(BigDecimal progressPercentage, BigDecimal requiredPercentage) {
        if (progressPercentage == null || requiredPercentage == null) {
            return false;
        }
        return progressPercentage.compareTo(requiredPercentage) >= 0;
    }
}
