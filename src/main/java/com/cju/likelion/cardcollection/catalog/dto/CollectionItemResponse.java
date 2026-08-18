package com.cju.likelion.cardcollection.catalog.dto;
import com.cju.likelion.cardcollection.catalog.domain.ProductCollectionItem;
public record CollectionItemResponse(ProductResponse product, boolean required, int displayOrder) { public static CollectionItemResponse from(ProductCollectionItem item) { return new CollectionItemResponse(ProductResponse.from(item.getProduct()), item.isRequired(), item.getDisplayOrder()); } }
