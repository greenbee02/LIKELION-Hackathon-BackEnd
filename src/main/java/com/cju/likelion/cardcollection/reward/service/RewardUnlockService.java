package com.cju.likelion.cardcollection.reward.service;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.catalog.domain.ProductCollection;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionItemRepository;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionRepository;
import com.cju.likelion.cardcollection.reward.domain.*;
import com.cju.likelion.cardcollection.reward.repository.CollectionRewardRepository;
import com.cju.likelion.cardcollection.reward.repository.UserRewardRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RewardUnlockService {
    private final ProductCollectionRepository collections;
    private final ProductCollectionItemRepository items;
    private final CardRepository cards;
    private final CollectionRewardRepository conditions;
    private final UserRewardRepository userRewards;
    public RewardUnlockService(ProductCollectionRepository collections, ProductCollectionItemRepository items, CardRepository cards, CollectionRewardRepository conditions, UserRewardRepository userRewards) { this.collections=collections; this.items=items; this.cards=cards; this.conditions=conditions; this.userRewards=userRewards; }

    public void evaluate(User user) {
        List<UUID> owned = cards.findByUserIdAndStatus(user.getId(), CardStatus.ACTIVE).stream().map(card -> card.getProduct().getId()).toList();
        for (ProductCollection collection : collections.findAll()) {
            List<UUID> required = items.findByProductCollectionId(collection.getId()).stream().filter(item -> item.isRequired()).map(item -> item.getProduct().getId()).toList();
            OfficialCollectionProgress progress = OfficialCollectionProgressCalculator.calculate(required, owned);
            for (CollectionReward condition : conditions.findByProductCollectionId(collection.getId())) {
                if (!RewardEligibilityEvaluator.isEligible(progress.percentage(), condition.getRequiredPercentage())) continue;
                if (condition.getReward() != null && condition.getReward().isActive() && !userRewards.existsByUserIdAndRewardId(user.getId(), condition.getReward().getId())) userRewards.save(UserReward.unlock(user, condition.getReward(), null));
                if (condition.getEvent() != null && condition.getEvent().isActive() && !userRewards.existsByUserIdAndEventId(user.getId(), condition.getEvent().getId())) userRewards.save(UserReward.unlock(user, null, condition.getEvent()));
            }
        }
    }
}
