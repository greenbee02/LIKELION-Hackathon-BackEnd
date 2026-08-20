package com.cju.likelion.cardcollection.reward.service;

import com.cju.likelion.cardcollection.auth.exception.InvalidCredentialsException;
import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.catalog.domain.ProductCollectionItem;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionItemRepository;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionRepository;
import com.cju.likelion.cardcollection.catalog.service.CatalogDomainException;
import com.cju.likelion.cardcollection.reward.domain.CollectionReward;
import com.cju.likelion.cardcollection.reward.dto.RewardCollectionDetailResponse;
import com.cju.likelion.cardcollection.reward.dto.RewardProgressResponse;
import com.cju.likelion.cardcollection.reward.repository.CollectionRewardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RewardProgressService {
    private final UserRepository users;
    private final CardRepository cards;
    private final ProductCollectionRepository collections;
    private final ProductCollectionItemRepository items;
    private final CollectionRewardRepository conditions;

    public RewardProgressService(UserRepository users, CardRepository cards,
                                 ProductCollectionRepository collections, ProductCollectionItemRepository items,
                                 CollectionRewardRepository conditions) {
        this.users = users;
        this.cards = cards;
        this.collections = collections;
        this.items = items;
        this.conditions = conditions;
    }

    @Transactional(readOnly = true)
    public List<RewardProgressResponse> list(UUID userId) {
        activeUser(userId);
        List<UUID> ownedProductIds = activeCards(userId).stream().map(card -> card.getProduct().getId()).toList();
        return collections.findAll().stream().map(collection -> {
            var progress = OfficialCollectionProgressCalculator.calculate(requiredProductIds(collection.getId()), ownedProductIds);
            var targets = conditions.findByProductCollectionId(collection.getId()).stream()
                    .map(condition -> progressTarget(condition, progress.percentage())).toList();
            return new RewardProgressResponse(collection.getId(), collection.getName(), progress.requiredProductCount(),
                    progress.ownedRequiredProductCount(), progress.percentage(), targets);
        }).toList();
    }

    /** 리워드 버튼 클릭용: 달성 카드, 아직 필요한 상품, 이벤트/보상 상세 정보를 반환한다. */
    @Transactional(readOnly = true)
    public RewardCollectionDetailResponse detail(UUID userId, UUID collectionId) {
        activeUser(userId);
        var collection = collections.findById(collectionId)
                .orElseThrow(() -> new CatalogDomainException("COLLECTION_NOT_FOUND", "공식 컬렉션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        List<Card> activeCards = activeCards(userId);
        Map<UUID, List<Card>> cardsByProduct = activeCards.stream()
                .collect(Collectors.groupingBy(card -> card.getProduct().getId()));
        List<ProductCollectionItem> requiredItems = requiredItems(collectionId);
        var progress = OfficialCollectionProgressCalculator.calculate(
                requiredItems.stream().map(item -> item.getProduct().getId()).toList(),
                activeCards.stream().map(card -> card.getProduct().getId()).toList());

        List<RewardCollectionDetailResponse.RequiredProduct> products = requiredItems.stream()
                .map(item -> requiredProduct(item, cardsByProduct.getOrDefault(item.getProduct().getId(), List.of())))
                .toList();
        List<RewardCollectionDetailResponse.UnlockTarget> targets = conditions.findByProductCollectionId(collectionId).stream()
                .map(condition -> detailTarget(condition, progress.percentage()))
                .toList();

        return new RewardCollectionDetailResponse(collection.getId(), collection.getName(), collection.getDescription(),
                collection.getCoverImageUrl(), progress.requiredProductCount(), progress.ownedRequiredProductCount(),
                progress.percentage(), products, targets);
    }

    private void activeUser(UUID userId) {
        users.findByIdAndDeletedAtIsNull(userId).orElseThrow(InvalidCredentialsException::new);
    }

    private List<Card> activeCards(UUID userId) {
        return cards.findByUserIdAndStatus(userId, CardStatus.ACTIVE);
    }

    private List<UUID> requiredProductIds(UUID collectionId) {
        return requiredItems(collectionId).stream().map(item -> item.getProduct().getId()).toList();
    }

    private List<ProductCollectionItem> requiredItems(UUID collectionId) {
        return items.findByProductCollectionId(collectionId).stream()
                .filter(ProductCollectionItem::isRequired)
                .sorted(Comparator.comparingInt(ProductCollectionItem::getDisplayOrder))
                .toList();
    }

    private RewardProgressResponse.UnlockTarget progressTarget(CollectionReward condition, BigDecimal percentage) {
        boolean reward = condition.getReward() != null;
        return new RewardProgressResponse.UnlockTarget(reward ? "REWARD" : "EVENT",
                reward ? condition.getReward().getId() : condition.getEvent().getId(),
                reward ? condition.getReward().getName() : condition.getEvent().getName(),
                condition.getRequiredPercentage(), RewardEligibilityEvaluator.isEligible(percentage, condition.getRequiredPercentage()));
    }

    private RewardCollectionDetailResponse.RequiredProduct requiredProduct(ProductCollectionItem item, List<Card> ownedCards) {
        var product = item.getProduct();
        List<RewardCollectionDetailResponse.OwnedCard> cardSnapshots = ownedCards.stream()
                .sorted(Comparator.comparing(Card::getPurchaseDate).reversed())
                .map(this::ownedCard)
                .toList();
        return new RewardCollectionDetailResponse.RequiredProduct(product.getId(), product.getName(),
                product.getOfferingType().name(), product.getCategory(), product.getImageUrl(), product.isLimited(),
                item.getDisplayOrder(), !cardSnapshots.isEmpty(), cardSnapshots);
    }

    private RewardCollectionDetailResponse.OwnedCard ownedCard(Card card) {
        var customization = card.getSelectedCustomization();
        String frontImageUrl = customization != null && hasText(customization.getGeneratedFrontImageUrl())
                ? customization.getGeneratedFrontImageUrl() : card.getTemplate().getFrontImageUrl();
        String backImageUrl = customization != null && hasText(customization.getGeneratedBackImageUrl())
                ? customization.getGeneratedBackImageUrl() : card.getTemplate().getBackImageUrl();
        return new RewardCollectionDetailResponse.OwnedCard(card.getId(), card.getCardType().name(),
                customization == null ? null : customization.getId(), frontImageUrl, backImageUrl,
                card.getPurchaseDate(), card.getIssuedAt(), card.getSerialNumber());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RewardCollectionDetailResponse.UnlockTarget detailTarget(CollectionReward condition, BigDecimal percentage) {
        boolean unlocked = RewardEligibilityEvaluator.isEligible(percentage, condition.getRequiredPercentage());
        if (condition.getReward() != null) {
            var reward = condition.getReward();
            return new RewardCollectionDetailResponse.UnlockTarget("REWARD", reward.getId(), reward.getName(),
                    reward.getDescription(), reward.getImageUrl(), condition.getRequiredPercentage(), unlocked,
                    new RewardCollectionDetailResponse.RewardInfo(reward.getRewardType(), reward.getQuantity(), reward.getExpiresAt()), null);
        }
        var event = condition.getEvent();
        return new RewardCollectionDetailResponse.UnlockTarget("EVENT", event.getId(), event.getName(),
                event.getDescription(), event.getImageUrl(), condition.getRequiredPercentage(), unlocked, null,
                new RewardCollectionDetailResponse.EventInfo(event.getLocation(), event.getStartAt(), event.getEndAt(),
                        event.getCapacity(), event.isActive()));
    }
}
