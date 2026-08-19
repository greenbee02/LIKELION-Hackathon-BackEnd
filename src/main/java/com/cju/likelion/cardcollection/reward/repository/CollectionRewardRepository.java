package com.cju.likelion.cardcollection.reward.repository;
import com.cju.likelion.cardcollection.reward.domain.CollectionReward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CollectionRewardRepository extends JpaRepository<CollectionReward, UUID> { List<CollectionReward> findByProductCollectionId(UUID id); }
