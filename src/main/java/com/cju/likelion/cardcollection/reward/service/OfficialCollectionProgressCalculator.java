package com.cju.likelion.cardcollection.reward.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 공식 컬렉션 리워드 달성률 계산을 DB 조회와 분리한 순수 도메인 로직이다.
 *
 * <p>같은 상품을 여러 번 구매해 카드가 여러 장이어도 상품 ID는 한 번만 인정한다.
 * 카드 상태 검증은 DB 조회 단계에서 수행하고, 이 계산기에는 리워드 계산에 인정되는
 * 보유 상품 ID만 전달한다.</p>
 */
public final class OfficialCollectionProgressCalculator {

    private OfficialCollectionProgressCalculator() {
    }

    public static OfficialCollectionProgress calculate(
            Collection<UUID> requiredProductIds,
            Collection<UUID> ownedProductIds
    ) {
        Set<UUID> requiredProducts = new HashSet<>(requiredProductIds);
        if (requiredProducts.isEmpty()) {
            return new OfficialCollectionProgress(0, 0, BigDecimal.ZERO);
        }

        Set<UUID> ownedRequiredProducts = new HashSet<>(ownedProductIds);
        ownedRequiredProducts.retainAll(requiredProducts);

        int requiredCount = requiredProducts.size();
        int ownedCount = ownedRequiredProducts.size();
        BigDecimal percentage = BigDecimal.valueOf(ownedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredCount), 2, RoundingMode.HALF_UP);

        return new OfficialCollectionProgress(requiredCount, ownedCount, percentage);
    }
}
