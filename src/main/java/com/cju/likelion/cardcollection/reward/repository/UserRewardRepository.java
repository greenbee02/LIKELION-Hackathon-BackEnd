package com.cju.likelion.cardcollection.reward.repository;
import com.cju.likelion.cardcollection.reward.domain.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRewardRepository extends JpaRepository<UserReward, UUID> { boolean existsByUserIdAndRewardId(UUID userId, UUID rewardId); boolean existsByUserIdAndEventId(UUID userId, UUID eventId); List<UserReward> findByUserId(UUID userId); }
