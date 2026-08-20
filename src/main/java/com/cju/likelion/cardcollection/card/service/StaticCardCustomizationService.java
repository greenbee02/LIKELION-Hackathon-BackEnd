package com.cju.likelion.cardcollection.card.service;

import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.card.domain.CardCustomizationLayer;
import com.cju.likelion.cardcollection.card.domain.CardCustomizationLayerType;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
import com.cju.likelion.cardcollection.card.dto.LayeredCustomizationCreateRequest;
import com.cju.likelion.cardcollection.card.dto.LayeredCustomizationResponse;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import com.cju.likelion.cardcollection.card.repository.CardCustomizationRepository;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.catalog.domain.CardBackLayout;
import com.cju.likelion.cardcollection.catalog.domain.CardDesignAsset;
import com.cju.likelion.cardcollection.catalog.domain.CardDesignAssetType;
import com.cju.likelion.cardcollection.catalog.repository.CardBackLayoutRepository;
import com.cju.likelion.cardcollection.catalog.repository.CardDesignAssetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StaticCardCustomizationService {

    private static final DateTimeFormatter PURCHASE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC);

    private final CardRepository cardRepository;
    private final CardCustomizationRepository customizationRepository;
    private final CardDesignAssetRepository assetRepository;
    private final CardBackLayoutRepository backLayoutRepository;
    private final ObjectMapper objectMapper;

    public StaticCardCustomizationService(
            CardRepository cardRepository,
            CardCustomizationRepository customizationRepository,
            CardDesignAssetRepository assetRepository,
            CardBackLayoutRepository backLayoutRepository,
            ObjectMapper objectMapper
    ) {
        this.cardRepository = cardRepository;
        this.customizationRepository = customizationRepository;
        this.assetRepository = assetRepository;
        this.backLayoutRepository = backLayoutRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LayeredCustomizationResponse create(
            UUID userId,
            UUID cardId,
            LayeredCustomizationCreateRequest request
    ) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        requireActive(card);

        CardDesignAsset productBackground = findAsset(request.productBackgroundAssetId());
        CardDesignAsset border = findAsset(request.borderAssetId());
        CardBackLayout backLayout = backLayoutRepository.findById(request.backLayoutId())
                .orElseThrow(() -> error("CARD_BACK_LAYOUT_NOT_FOUND", "카드 뒷면 레이아웃을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        UUID productId = card.getProduct().getId();
        UUID brandId = card.getProduct().getBrand().getId();
        validateProductBackground(productBackground, productId, brandId);
        validateBorder(border, brandId);
        validateBackLayout(backLayout, brandId);

        Instant now = Instant.now();
        String styleData = writeJson(request.text().style() == null ? Map.of() : request.text().style());
        String backContentData = writeJson(backContent(card));
        String customizationData = writeJson(Map.of(
                "source", "approved-assets",
                "productBackgroundAssetId", productBackground.getId(),
                "borderAssetId", border.getId(),
                "backLayoutId", backLayout.getId()
        ));

        CardCustomization customization = CardCustomization.layered(
                card,
                card.getTemplate(),
                backLayout,
                request.text().content().trim(),
                customizationData,
                backContentData,
                now
        );
        customization.addLayer(CardCustomizationLayer.assetLayer(
                customization,
                productBackground,
                CardCustomizationLayerType.PRODUCT_BACKGROUND,
                0,
                10
        ));
        customization.addLayer(CardCustomizationLayer.assetLayer(
                customization,
                border,
                CardCustomizationLayerType.BORDER,
                1,
                20
        ));
        customization.addLayer(CardCustomizationLayer.textLayer(
                customization,
                request.text().content().trim(),
                request.text().x(),
                request.text().y(),
                request.text().width(),
                request.text().height(),
                request.text().rotation(),
                request.text().opacity(),
                request.text().zIndex(),
                styleData
        ));

        CardCustomization saved = customizationRepository.save(customization);
        card.selectCustomization(saved);
        return toResponse(saved);
    }

    private CardDesignAsset findAsset(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> error("CARD_DESIGN_ASSET_NOT_FOUND", "카드 디자인 에셋을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void validateProductBackground(CardDesignAsset asset, UUID productId, UUID brandId) {
        if (!asset.isActive()) {
            throw error("CARD_DESIGN_ASSET_INACTIVE", "비활성화된 카드 디자인 에셋입니다.", HttpStatus.CONFLICT);
        }
        if (asset.getAssetType() != CardDesignAssetType.PRODUCT_BACKGROUND) {
            throw error("CARD_DESIGN_ASSET_TYPE_MISMATCH", "상품 배경에는 PRODUCT_BACKGROUND 에셋만 사용할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        if (asset.getProduct() == null || !asset.getProduct().getId().equals(productId)) {
            throw error("CARD_DESIGN_ASSET_PRODUCT_MISMATCH", "현재 카드 상품의 배경 에셋만 사용할 수 있습니다.", HttpStatus.CONFLICT);
        }
        requireSameBrand(asset.getBrand().getId(), brandId, "CARD_DESIGN_ASSET_BRAND_MISMATCH");
    }

    private void validateBorder(CardDesignAsset asset, UUID brandId) {
        if (!asset.isActive()) {
            throw error("CARD_DESIGN_ASSET_INACTIVE", "비활성화된 카드 디자인 에셋입니다.", HttpStatus.CONFLICT);
        }
        if (asset.getAssetType() != CardDesignAssetType.BORDER) {
            throw error("CARD_DESIGN_ASSET_TYPE_MISMATCH", "테두리에는 BORDER 에셋만 사용할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        requireSameBrand(asset.getBrand().getId(), brandId, "CARD_DESIGN_ASSET_BRAND_MISMATCH");
    }

    private void validateBackLayout(CardBackLayout layout, UUID brandId) {
        if (!layout.isActive() || !layout.getBaseAsset().isActive()) {
            throw error("CARD_BACK_LAYOUT_INACTIVE", "비활성화된 카드 뒷면 레이아웃입니다.", HttpStatus.CONFLICT);
        }
        requireSameBrand(layout.getBrand().getId(), brandId, "CARD_BACK_LAYOUT_BRAND_MISMATCH");
        if (layout.getBaseAsset().getAssetType() != CardDesignAssetType.BACK_BASE) {
            throw error("CARD_BACK_LAYOUT_INVALID", "카드 뒷면 레이아웃의 베이스 에셋이 올바르지 않습니다.", HttpStatus.CONFLICT);
        }
    }

    private void requireSameBrand(UUID actualBrandId, UUID expectedBrandId, String code) {
        if (!actualBrandId.equals(expectedBrandId)) {
            throw error(code, "카드 상품과 같은 브랜드의 에셋 및 레이아웃만 사용할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }

    private void requireActive(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw error("CARD_NOT_ACTIVE", "활성 상태의 카드만 커스터마이징할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }

    private Map<String, Object> backContent(Card card) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("store", card.getPurchaseStore().getName());
        content.put("date", PURCHASE_DATE_FORMAT.format(card.getPurchaseDate()));
        content.put("location", card.getPurchaseStore().getCity() + ", " + card.getPurchaseStore().getCountry());
        content.put("product", card.getProduct().getName());
        content.put("serialNumber", card.getSerialNumber());
        return content;
    }

    private LayeredCustomizationResponse toResponse(CardCustomization customization) {
        List<LayeredCustomizationResponse.FrontLayerResponse> layers = customization.getLayers().stream()
                .map(layer -> new LayeredCustomizationResponse.FrontLayerResponse(
                        layer.getLayerType().name(),
                        layer.getAsset() == null ? null : layer.getAsset().getId(),
                        layer.getAsset() == null ? null : layer.getAsset().getImageUrl(),
                        layer.getTextContent(),
                        layer.getLayerOrder(),
                        layer.getPositionX(),
                        layer.getPositionY(),
                        layer.getWidth(),
                        layer.getHeight(),
                        layer.getRotation(),
                        layer.getOpacity(),
                        layer.getZIndex(),
                        readJson(layer.getStyleData())
                ))
                .toList();
        CardBackLayout backLayout = customization.getBackLayout();
        LayeredCustomizationResponse.BackResponse back = new LayeredCustomizationResponse.BackResponse(
                backLayout.getId(),
                backLayout.getBaseAsset().getImageUrl(),
                readJson(backLayout.getLayoutData()),
                readJson(customization.getBackContentData())
        );
        return new LayeredCustomizationResponse(
                customization.getId(),
                customization.getCard().getId(),
                customization.getGenerationStatus().name(),
                layers,
                back,
                customization.getCreatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw error("CARD_CUSTOMIZATION_DATA_INVALID", "커스터마이징 데이터를 저장할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value == null || value.isBlank() ? "{}" : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 커스터마이징 JSON 데이터가 올바르지 않습니다.", exception);
        }
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }
}
