package com.cju.likelion.cardcollection.collection.domain;

import com.cju.likelion.cardcollection.card.domain.Card;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name = "collection_cards") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionCard {
    @EmbeddedId private CollectionCardId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("collectionId") @JoinColumn(name = "collection_id") private UserCollection collection;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("cardId") @JoinColumn(name = "card_id") private Card card;
    @Column(name = "added_at", nullable = false) private Instant addedAt;
    public static CollectionCard of(UserCollection collection, Card card) { CollectionCard link = new CollectionCard(); link.id = new CollectionCardId(collection.getId(), card.getId()); link.collection = collection; link.card = card; link.addedAt = Instant.now(); return link; }
}
