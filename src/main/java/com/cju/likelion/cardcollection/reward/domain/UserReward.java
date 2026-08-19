package com.cju.likelion.cardcollection.reward.domain;
import com.cju.likelion.cardcollection.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="user_rewards") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class UserReward { @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reward_id") private Reward reward; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="event_id") private RewardEvent event; @Enumerated(EnumType.STRING) @Column(nullable=false) private UserRewardStatus status; @Column(name="unlocked_at",nullable=false) private Instant unlockedAt; @Column(name="claim_code") private String claimCode; @Column(name="expires_at") private Instant expiresAt; public static UserReward unlock(User u, Reward r, RewardEvent e){UserReward x=new UserReward();x.id=UUID.randomUUID();x.user=u;x.reward=r;x.event=e;x.status=UserRewardStatus.UNLOCKED;x.unlockedAt=Instant.now();x.expiresAt=r==null?e.getEndAt():r.getExpiresAt();return x;} public void issueClaimCode(){if(claimCode==null)claimCode="CLAIM-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();} }
