package com.cju.likelion.cardcollection.catalog.service;

import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.ProductCollection;
import com.cju.likelion.cardcollection.catalog.dto.*;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionItemRepository;
import com.cju.likelion.cardcollection.catalog.repository.ProductCollectionRepository;
import com.cju.likelion.cardcollection.catalog.repository.ProductRepository;
import com.cju.likelion.cardcollection.catalog.repository.CardTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Stream;

@Service
public class CatalogService {
    private final ProductRepository productRepository; private final ProductCollectionRepository collectionRepository; private final ProductCollectionItemRepository itemRepository; private final CardTemplateRepository templateRepository;
    public CatalogService(ProductRepository productRepository, ProductCollectionRepository collectionRepository, ProductCollectionItemRepository itemRepository, CardTemplateRepository templateRepository) { this.productRepository = productRepository; this.collectionRepository = collectionRepository; this.itemRepository = itemRepository; this.templateRepository = templateRepository; }
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> products(String offeringType, String category, String theme, String season, String region, Boolean limited, int page, int size) {
        Stream<Product> stream = productRepository.findByActiveTrueOrderByCreatedAtDesc().stream();
        if (offeringType != null) stream = stream.filter(p -> p.getOfferingType().name().equalsIgnoreCase(offeringType));
        if (category != null) stream = stream.filter(p -> category.equalsIgnoreCase(p.getCategory()));
        if (theme != null) stream = stream.filter(p -> theme.equalsIgnoreCase(p.getTheme()));
        if (season != null) stream = stream.filter(p -> season.equalsIgnoreCase(p.getSeason()));
        if (region != null) stream = stream.filter(p -> region.equalsIgnoreCase(p.getRegion()));
        if (limited != null) stream = stream.filter(p -> p.isLimited() == limited);
        List<ProductResponse> all = stream.map(ProductResponse::from).toList(); int start = Math.min(page * size, all.size()); int end = Math.min(start + size, all.size());
        return new PageResponse<>(all.subList(start, end), page, size, all.size(), (int) Math.ceil((double) all.size() / size));
    }
    @Transactional(readOnly = true) public ProductResponse product(UUID id) { return ProductResponse.from(productRepository.findById(id).filter(Product::isActive).orElseThrow(() -> error("PRODUCT_NOT_FOUND", "상품 또는 경험을 찾을 수 없습니다."))); }
    @Transactional(readOnly = true) public List<ProductCollectionResponse> collections() { return collectionRepository.findAll().stream().map(ProductCollectionResponse::from).toList(); }
    @Transactional(readOnly = true) public ProductCollectionResponse collection(UUID id) { return ProductCollectionResponse.from(findCollection(id)); }
    @Transactional(readOnly = true) public List<CollectionItemResponse> collectionProducts(UUID id) { findCollection(id); return itemRepository.findByProductCollectionId(id).stream().sorted(Comparator.comparingInt(i -> i.getDisplayOrder())).map(CollectionItemResponse::from).toList(); }
    @Transactional(readOnly = true) public List<CardTemplateResponse> templates() { return templateRepository.findAllByActiveTrueOrderByCreatedAtAsc().stream().map(CardTemplateResponse::from).toList(); }
    private ProductCollection findCollection(UUID id) { return collectionRepository.findById(id).orElseThrow(() -> error("COLLECTION_NOT_FOUND", "공식 컬렉션을 찾을 수 없습니다.")); }
    private CatalogDomainException error(String code, String message) { return new CatalogDomainException(code, message, HttpStatus.NOT_FOUND); }
}
