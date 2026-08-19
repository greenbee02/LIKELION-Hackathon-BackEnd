package com.cju.likelion.cardcollection.reward.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialCollectionProgressCalculatorTest {

    @Test
    void seoulExclusiveTwoOfThreeRequiredProductsIsSixtySixPointSixtySevenPercent() {
        // DataBase/seed_data.sql: Seoul Exclusive = products 003, 004, 008
        List<UUID> requiredProducts = List.of(product("003"), product("004"), product("008"));
        List<UUID> ownedProducts = List.of(product("003"), product("004"));

        OfficialCollectionProgress progress = OfficialCollectionProgressCalculator.calculate(
                requiredProducts, ownedProducts);

        assertThat(progress.requiredProductCount()).isEqualTo(3);
        assertThat(progress.ownedRequiredProductCount()).isEqualTo(2);
        assertThat(progress.percentage()).isEqualByComparingTo("66.67");
    }

    @Test
    void repeatedCardsForTheSameProductCountOnlyOnce() {
        // DataBase/seed_data.sql: Women's Signature = products 001, 002, 003, 004, 009
        List<UUID> requiredProducts = List.of(
                product("001"), product("002"), product("003"), product("004"), product("009"));
        List<UUID> ownedProducts = List.of(
                product("001"), product("001"), product("002"), product("003"));

        OfficialCollectionProgress progress = OfficialCollectionProgressCalculator.calculate(
                requiredProducts, ownedProducts);

        assertThat(progress.requiredProductCount()).isEqualTo(5);
        assertThat(progress.ownedRequiredProductCount()).isEqualTo(3);
        assertThat(progress.percentage()).isEqualByComparingTo("60.00");
    }

    @Test
    void productsOutsideTheOfficialCollectionDoNotIncreaseProgress() {
        List<UUID> requiredProducts = List.of(product("010"), product("011"));
        List<UUID> ownedProducts = List.of(product("010"), product("005"));

        OfficialCollectionProgress progress = OfficialCollectionProgressCalculator.calculate(
                requiredProducts, ownedProducts);

        assertThat(progress.ownedRequiredProductCount()).isEqualTo(1);
        assertThat(progress.percentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void collectionWithoutRequiredProductsHasZeroProgress() {
        OfficialCollectionProgress progress = OfficialCollectionProgressCalculator.calculate(List.of(), List.of(product("001")));

        assertThat(progress.requiredProductCount()).isZero();
        assertThat(progress.ownedRequiredProductCount()).isZero();
        assertThat(progress.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private UUID product(String suffix) {
        return UUID.fromString("50000000-0000-0000-0000-000000000" + suffix);
    }
}
