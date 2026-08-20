package com.cju.likelion.cardcollection.reward.domain;
import com.cju.likelion.cardcollection.catalog.domain.Brand;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="rewards") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Reward { @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="brand_id") private Brand brand; @Column(nullable=false) private String name; @Column(columnDefinition="TEXT") private String description; @Column(name="reward_type",nullable=false) private String rewardType; @Column(name="image_url") private String imageUrl; private Integer quantity; @Column(name="is_active",nullable=false) private boolean active; @Column(name="expires_at") private Instant expiresAt; }
