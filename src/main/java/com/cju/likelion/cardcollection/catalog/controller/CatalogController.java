package com.cju.likelion.cardcollection.catalog.controller;

import com.cju.likelion.cardcollection.catalog.dto.*;
import com.cju.likelion.cardcollection.catalog.service.CatalogService;
import com.cju.likelion.cardcollection.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }
    @GetMapping("/products") public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> products(@RequestParam(required = false) String offeringType, @RequestParam(required = false) String category, @RequestParam(required = false) String theme, @RequestParam(required = false) String season, @RequestParam(required = false) String region, @RequestParam(required = false) Boolean limited, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page와 size 값이 올바르지 않습니다."); return ResponseEntity.ok(new ApiResponse<>(service.products(offeringType, category, theme, season, region, limited, page, size))); }
    @GetMapping("/products/{productId}") public ResponseEntity<ApiResponse<ProductResponse>> product(@PathVariable UUID productId) { return ResponseEntity.ok(new ApiResponse<>(service.product(productId))); }
    @GetMapping("/product-collections") public ResponseEntity<ApiResponse<List<ProductCollectionResponse>>> collections() { return ResponseEntity.ok(new ApiResponse<>(service.collections())); }
    @GetMapping("/product-collections/{collectionId}") public ResponseEntity<ApiResponse<ProductCollectionResponse>> collection(@PathVariable UUID collectionId) { return ResponseEntity.ok(new ApiResponse<>(service.collection(collectionId))); }
    @GetMapping("/product-collections/{collectionId}/products") public ResponseEntity<ApiResponse<List<CollectionItemResponse>>> collectionProducts(@PathVariable UUID collectionId) { return ResponseEntity.ok(new ApiResponse<>(service.collectionProducts(collectionId))); }
    @GetMapping("/card-templates") public ResponseEntity<ApiResponse<List<CardTemplateResponse>>> templates() { return ResponseEntity.ok(new ApiResponse<>(service.templates())); }
}
