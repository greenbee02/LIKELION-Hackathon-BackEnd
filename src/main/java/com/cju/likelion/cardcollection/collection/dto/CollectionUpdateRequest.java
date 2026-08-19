package com.cju.likelion.cardcollection.collection.dto;
import jakarta.validation.constraints.*;
public record CollectionUpdateRequest(@NotBlank @Size(max = 255) String name, String description, @Size(max = 1000) String coverImageUrl) {}
