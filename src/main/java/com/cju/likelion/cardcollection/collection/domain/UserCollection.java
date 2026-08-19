package com.cju.likelion.cardcollection.collection.domain;

import com.cju.likelion.cardcollection.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCollection {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 255) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "cover_image_url", length = 1000) private String coverImageUrl;
    @Enumerated(EnumType.STRING) @Column(name = "collection_type", nullable = false, length = 30) private CollectionType collectionType;
    @Column(name = "generation_reason", columnDefinition = "TEXT") private String generationReason;
    @Column(name = "is_public", nullable = false) private boolean isPublic;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static UserCollection custom(User user, String name, String description, String coverImageUrl) {
        UserCollection collection = new UserCollection();
        collection.id = UUID.randomUUID(); collection.user = user; collection.name = name;
        collection.description = description; collection.coverImageUrl = coverImageUrl;
        collection.collectionType = CollectionType.CUSTOM; collection.isPublic = false;
        collection.createdAt = Instant.now(); collection.updatedAt = collection.createdAt;
        return collection;
    }
    public void update(String name, String description, String coverImageUrl) {
        this.name = name; this.description = description; this.coverImageUrl = coverImageUrl;
    }
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); if (updatedAt == null) updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
