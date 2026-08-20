package com.cju.likelion.cardcollection.reward.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 리워드 목록에서 공식 컬렉션 하나를 선택했을 때 표시할 데이터다.
 * requiredProducts는 달성률 계산 대상만 반환하고, 같은 상품의 여러 카드는 모두
 * cards에 보존한다. 달성률은 cards 개수가 아닌 owned 값으로 계산한다.
 */
public record RewardCollectionDetailResponse(
        UUID collectionId,
        String collectionName,
        String collectionDescription,
        String coverImageUrl,
        int requiredProductCount,
        int ownedRequiredProductCount,
        BigDecimal percentage,
        List<RequiredProduct> requiredProducts,
        List<UnlockTarget> targets
) {
    public record RequiredProduct(
            UUID productId,
            String name,
            String offeringType,
            String category,
            String imageUrl,
            boolean limited,
            int displayOrder,
            boolean owned,
            List<OwnedCard> cards
    ) {}

    public record OwnedCard(
            UUID cardId,
            String cardType,
            UUID selectedCustomizationId,
            String frontImageUrl,
            String backImageUrl,
            Instant purchaseDate,
            Instant issuedAt,
            String serialNumber
    ) {}

    public record UnlockTarget(
            String type,
            UUID id,
            String name,
            String description,
            String imageUrl,
            BigDecimal requiredPercentage,
            boolean unlocked,
            RewardInfo reward,
            EventInfo event
    ) {}

    public record RewardInfo(
            String rewardType,
            Integer quantity,
            Instant expiresAt
    ) {}

    public record EventInfo(
            String location,
            Instant startAt,
            Instant endAt,
            Integer capacity,
            boolean active
    ) {}
}
