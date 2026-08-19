package com.cju.likelion.cardcollection.collection.dto;
import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CollectionCardAddRequest(@NotNull UUID cardId) {}
