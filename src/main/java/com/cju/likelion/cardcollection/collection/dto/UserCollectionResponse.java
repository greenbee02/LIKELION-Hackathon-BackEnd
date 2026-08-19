package com.cju.likelion.cardcollection.collection.dto;
import com.cju.likelion.cardcollection.card.dto.CardResponse;
import com.cju.likelion.cardcollection.collection.domain.UserCollection;
import java.time.Instant; import java.util.*;
public record UserCollectionResponse(UUID id, String name, String description, String coverImageUrl, String collectionType, Instant createdAt, Instant updatedAt, long cardCount, List<CardResponse> cards) {
    public static UserCollectionResponse summary(UserCollection c, long count) { return new UserCollectionResponse(c.getId(), c.getName(), c.getDescription(), c.getCoverImageUrl(), c.getCollectionType().name(), c.getCreatedAt(), c.getUpdatedAt(), count, null); }
    public static UserCollectionResponse detail(UserCollection c, List<CardResponse> cards) { return new UserCollectionResponse(c.getId(), c.getName(), c.getDescription(), c.getCoverImageUrl(), c.getCollectionType().name(), c.getCreatedAt(), c.getUpdatedAt(), cards.size(), cards); }
}
