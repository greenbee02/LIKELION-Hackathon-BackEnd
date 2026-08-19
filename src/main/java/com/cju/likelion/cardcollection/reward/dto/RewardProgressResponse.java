package com.cju.likelion.cardcollection.reward.dto;
import java.math.BigDecimal; import java.util.*;
public record RewardProgressResponse(UUID collectionId,String collectionName,int requiredProductCount,int ownedRequiredProductCount,BigDecimal percentage,List<UnlockTarget> targets){public record UnlockTarget(String type,UUID id,String name,BigDecimal requiredPercentage,boolean unlocked){}}
