package com.cju.likelion.cardcollection.reward.domain;
import com.cju.likelion.cardcollection.catalog.domain.Brand;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="events") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class RewardEvent { @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="brand_id") private Brand brand; @Column(nullable=false) private String name; @Column(columnDefinition="TEXT") private String description; private String location; @Column(name="start_at",nullable=false) private Instant startAt; @Column(name="end_at",nullable=false) private Instant endAt; @Column(name="is_active",nullable=false) private boolean active; }
