package com.cju.likelion.cardcollection.collection.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CollectionCardId implements Serializable {
    private UUID collectionId;
    private UUID cardId;
    protected CollectionCardId() {}
    public CollectionCardId(UUID collectionId, UUID cardId) { this.collectionId = collectionId; this.cardId = cardId; }
    public UUID getCollectionId() { return collectionId; }
    public UUID getCardId() { return cardId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof CollectionCardId other)) return false; return Objects.equals(collectionId, other.collectionId) && Objects.equals(cardId, other.cardId); }
    @Override public int hashCode() { return Objects.hash(collectionId, cardId); }
}
