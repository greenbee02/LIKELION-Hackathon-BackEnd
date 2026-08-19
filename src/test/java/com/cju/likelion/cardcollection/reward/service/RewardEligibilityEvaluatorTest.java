package com.cju.likelion.cardcollection.reward.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RewardEligibilityEvaluatorTest {

    @Test
    void seoulExclusiveTwoOfThreeProductsUnlocksTheSixtySixPointSixtySevenCondition() {
        // DataBase/seed_data.sql: Seoul Exclusive event condition = 66.67%
        assertThat(RewardEligibilityEvaluator.isEligible(
                new BigDecimal("66.67"), new BigDecimal("66.67"))).isTrue();
    }

    @Test
    void seoulExclusiveTwoOfThreeProductsDoesNotUnlockTheOneHundredPercentReward() {
        // DataBase/seed_data.sql: Seoul Collector Pass condition = 100.00%
        assertThat(RewardEligibilityEvaluator.isEligible(
                new BigDecimal("66.67"), new BigDecimal("100.00"))).isFalse();
    }

    @Test
    void womensSignatureThreeOfFiveProductsUnlocksTheSixtyPercentCondition() {
        assertThat(RewardEligibilityEvaluator.isEligible(
                new BigDecimal("60.00"), new BigDecimal("60.00"))).isTrue();
    }
}
