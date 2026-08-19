package com.cju.likelion.cardcollection.reward.service;

import java.math.BigDecimal;

public record OfficialCollectionProgress(
        int requiredProductCount,
        int ownedRequiredProductCount,
        BigDecimal percentage
) {
}
