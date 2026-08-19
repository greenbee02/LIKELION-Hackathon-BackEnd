package com.cju.likelion.cardcollection.reward.domain;
import com.cju.likelion.cardcollection.catalog.domain.ProductCollection;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="collection_rewards") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class CollectionReward { @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_collection_id") private ProductCollection productCollection; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reward_id") private Reward reward; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="event_id") private RewardEvent event; @Column(name="required_percentage",nullable=false) private BigDecimal requiredPercentage; }
