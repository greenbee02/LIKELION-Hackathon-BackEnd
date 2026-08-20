package com.cju.likelion.cardcollection.card.service;

import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
import com.cju.likelion.cardcollection.card.dto.CardCustomizationOptionsResponse;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
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

import java.util.List;
import java.util.UUID;

@Service
public class CardDesignAssetQueryService {

    private final CardRepository cardRepository;
    private final CardDesignAssetRepository assetRepository;
    private final CardBackLayoutRepository backLayoutRepository;
    private final ObjectMapper objectMapper;

    public CardDesignAssetQueryService(
            CardRepository cardRepository,
            CardDesignAssetRepository assetRepository,
            CardBackLayoutRepository backLayoutRepository,
            ObjectMapper objectMapper
    ) {
        this.cardRepository = cardRepository;
        this.assetRepository = assetRepository;
        this.backLayoutRepository = backLayoutRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CardCustomizationOptionsResponse get(UUID userId, UUID cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw error("CARD_NOT_ACTIVE", "활성 상태의 카드만 디자인 에셋을 조회할 수 있습니다.", HttpStatus.CONFLICT);
        }

        UUID productId = card.getProduct().getId();
        UUID brandId = card.getProduct().getBrand().getId();
        List<CardCustomizationOptionsResponse.DesignAssetResponse> productBackgrounds = assetRepository
                .findByProductIdAndAssetTypeAndActiveTrueOrderByVariantCodeAsc(
                        productId,
                        CardDesignAssetType.PRODUCT_BACKGROUND
                )
                .stream()
                .map(this::toAssetResponse)
                .toList();
        List<CardCustomizationOptionsResponse.DesignAssetResponse> borders = assetRepository
                .findByBrandIdAndAssetTypeAndActiveTrueOrderByVariantCodeAsc(brandId, CardDesignAssetType.BORDER)
                .stream()
                .map(this::toAssetResponse)
                .toList();

        CardBackLayout backLayout = backLayoutRepository.findByBrandIdAndActiveTrueOrderByCreatedAtAsc(brandId)
                .stream()
                .findFirst()
                .orElseThrow(() -> error(
                        "CARD_BACK_LAYOUT_NOT_FOUND",
                        "사용 가능한 카드 뒷면 레이아웃이 없습니다.",
                        HttpStatus.CONFLICT
                ));
        CardCustomizationOptionsResponse.BackOptionsResponse back = CardCustomizationOptionsResponse.BackOptionsResponse.from(
                backLayout,
                readJson(backLayout.getLayoutData())
        );

        return new CardCustomizationOptionsResponse(
                cardId,
                productId,
                new CardCustomizationOptionsResponse.FrontOptionsResponse(productBackgrounds, borders),
                back
        );
    }

    private CardCustomizationOptionsResponse.DesignAssetResponse toAssetResponse(CardDesignAsset asset) {
        return CardCustomizationOptionsResponse.DesignAssetResponse.from(asset, readJson(asset.getMetadata()));
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value == null || value.isBlank() ? "{}" : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("카드 디자인 에셋 JSON 데이터가 올바르지 않습니다.", exception);
        }
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }
}
