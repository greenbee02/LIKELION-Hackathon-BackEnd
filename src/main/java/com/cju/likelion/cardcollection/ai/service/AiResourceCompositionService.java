package com.cju.likelion.cardcollection.ai.service;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionResponse;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.card.domain.CardLayerType;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
import com.cju.likelion.cardcollection.card.dto.CardLayerRequest;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import com.cju.likelion.cardcollection.card.repository.CardCustomizationRepository;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.card.dto.CardCustomizationResponse;
import com.cju.likelion.cardcollection.card.dto.CardResponse;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiResourceCompositionService {

    private final CardRepository cardRepository;
    private final AiResourceGenerationRepository resourceRepository;
    private final CardCustomizationRepository customizationRepository;
    private final ObjectMapper objectMapper;

    public AiResourceCompositionService(
            CardRepository cardRepository,
            AiResourceGenerationRepository resourceRepository,
            CardCustomizationRepository customizationRepository,
            ObjectMapper objectMapper
    ) {
        this.cardRepository = cardRepository;
        this.resourceRepository = resourceRepository;
        this.customizationRepository = customizationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiResourceCompositionResponse compose(
            UUID userId,
            UUID cardId,
            AiResourceCompositionRequest request
    ) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw error("CARD_NOT_ACTIVE", "현재 상태의 카드는 커스터마이징할 수 없습니다.", HttpStatus.CONFLICT);
        }
        if (new HashSet<>(request.resourceIds()).size() != request.resourceIds().size()) {
            throw error("AI_RESOURCE_DUPLICATED", "같은 AI 리소스를 중복 선택할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<AiResourceGeneration> resources = request.resourceIds().stream()
                .map(resourceId -> resourceRepository.findByIdAndCardId(resourceId, cardId)
                        .orElseThrow(() -> error(
                                "AI_RESOURCE_NOT_FOUND",
                                "카드에 연결된 AI 리소스를 찾을 수 없습니다.",
                                HttpStatus.NOT_FOUND
                        )))
                .toList();

        if (resources.stream().anyMatch(resource ->
                resource.getGenerationStatus() != AiResourceStatus.COMPLETED)) {
            throw error(
                    "AI_RESOURCE_NOT_COMPLETED",
                    "완료된 AI 리소스만 카드에 조합할 수 있습니다.",
                    HttpStatus.CONFLICT
            );
        }

        Set<UUID> selectedCandidateGroups = new HashSet<>();
        if (resources.stream()
                .map(AiResourceGeneration::getCandidateGroupId)
                .filter(java.util.Objects::nonNull)
                .anyMatch(groupId -> !selectedCandidateGroups.add(groupId))) {
            throw error(
                    "AI_RESOURCE_CANDIDATE_DUPLICATED",
                    "같은 리소스 종류의 후보는 한 번에 하나만 선택할 수 있습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String customizationData = serializeComposition(card, resources, request.layoutData(), request.layers());
        CardTemplate template = card.getTemplate();
        String generatedFrontImageUrl = resources.stream()
                .filter(resource -> resource.getResourceType() == com.cju.likelion.cardcollection.ai.domain.AiResourceType.BACKGROUND)
                .map(AiResourceGeneration::getGeneratedImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(template.getFrontImageUrl());
        CardCustomization customization = CardCustomization.composed(
                card,
                template,
                generatedFrontImageUrl,
                template.getBackImageUrl(),
                request.message(),
                request.message(),
                customizationData,
                Instant.now()
        );
        CardCustomization saved = customizationRepository.save(customization);
        card.selectCustomization(saved);

        return new AiResourceCompositionResponse(
                CardResponse.from(card),
                CardCustomizationResponse.from(saved)
        );
    }

    private String serializeComposition(
            Card card,
            List<AiResourceGeneration> resources,
            Map<String, Object> layoutData,
            List<CardLayerRequest> requestedLayers
    ) {
        Map<String, Object> composition = new LinkedHashMap<>();
        composition.put("version", "composition-v2");
        composition.put("resourceIds", resources.stream().map(AiResourceGeneration::getId).toList());
        composition.put("resources", resources.stream().map(resource -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", resource.getId());
            item.put("type", resource.getResourceType().name());
            item.put("generatedImageUrl", resource.getGeneratedImageUrl());
            item.put("generatedData", resource.getGeneratedData());
            return item;
        }).toList());
        composition.put("layers", buildLayers(card, resources, requestedLayers));
        composition.put("layoutData", layoutData);
        try {
            return objectMapper.writeValueAsString(composition);
        } catch (JsonProcessingException exception) {
            throw error("AI_COMPOSITION_INVALID", "AI 리소스 조합 데이터를 저장할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Map<String, Object>> buildLayers(
            Card card,
            List<AiResourceGeneration> resources,
            List<CardLayerRequest> requestedLayers
    ) {
        Map<UUID, AiResourceGeneration> resourcesById = resources.stream()
                .collect(java.util.stream.Collectors.toMap(AiResourceGeneration::getId, resource -> resource));
        List<Map<String, Object>> defaultLayers = new ArrayList<>();
        defaultLayers.add(defaultBackgroundLayer(card));

        if (requestedLayers == null || requestedLayers.isEmpty()) {
            return defaultLayers;
        }

        Set<String> layerIds = new HashSet<>();
        Set<String> replacedSlots = requestedLayers.stream()
                .map(this::layerSlot)
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> layers = defaultLayers.stream()
                .filter(layer -> !replacedSlots.contains(layer.get("slot")))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (CardLayerRequest layer : requestedLayers) {
            validateLayer(layer, layerIds, resourcesById, card);
            layers.add(toLayerData(layer, resourcesById.get(layer.resourceId()), card));
        }
        return layers;
    }

    private Map<String, Object> defaultBackgroundLayer(Card card) {
        Map<String, Object> layer = new LinkedHashMap<>();
        layer.put("id", "template-background");
        layer.put("slot", "BACKGROUND");
        layer.put("type", CardLayerType.BACKGROUND.name());
        layer.put("sourceType", "TEMPLATE");
        layer.put("assetUrl", card.getTemplate().getFrontImageUrl());
        layer.put("x", BigDecimal.ZERO);
        layer.put("y", BigDecimal.ZERO);
        layer.put("width", BigDecimal.ONE);
        layer.put("height", BigDecimal.ONE);
        layer.put("rotation", BigDecimal.ZERO);
        layer.put("opacity", BigDecimal.ONE);
        layer.put("zIndex", 0);
        layer.put("visible", true);
        layer.put("locked", false);
        layer.put("replaceable", true);
        layer.put("isDefault", true);
        return layer;
    }

    private void validateLayer(
            CardLayerRequest layer,
            Set<String> layerIds,
            Map<UUID, AiResourceGeneration> resourcesById,
            Card card
    ) {
        if (!layerIds.add(layer.id())) {
            throw error("CARD_LAYER_DUPLICATED", "같은 레이어 id를 중복 사용할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if ("base-card".equals(layer.id()) || layer.type() == CardLayerType.BASE_CARD) {
            throw error("CARD_BASE_LAYER_DEPRECATED", "기본 카드 전체가 아닌 레이어 슬롯을 교체해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (layer.width().compareTo(BigDecimal.ZERO) <= 0 || layer.height().compareTo(BigDecimal.ZERO) <= 0) {
            throw error("CARD_LAYER_SIZE_INVALID", "레이어 width와 height는 0보다 커야 합니다.", HttpStatus.BAD_REQUEST);
        }

        AiResourceGeneration resource = layer.resourceId() == null
                ? null
                : resourcesById.get(layer.resourceId());
        if (layer.resourceId() != null && resource == null) {
            throw error("CARD_LAYER_RESOURCE_NOT_SELECTED", "레이어가 선택한 AI 리소스를 참조하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        if (layer.type() == CardLayerType.TEXT) {
            if (layer.resourceId() != null || layer.text() == null || layer.text().isBlank()) {
                throw error("CARD_TEXT_LAYER_INVALID", "TEXT 레이어에는 문구만 입력해야 합니다.", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (layer.type() == CardLayerType.FINISH && layer.resourceId() == null) {
            return;
        }
        if (layer.type() == CardLayerType.PRODUCT && layer.resourceId() == null) {
            if (card.getProduct().getImageUrl() == null || card.getProduct().getImageUrl().isBlank()) {
                throw error("PRODUCT_IMAGE_NOT_FOUND", "상품 레이어에 사용할 상품 이미지가 없습니다.", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (resource == null) {
            throw error("CARD_LAYER_RESOURCE_REQUIRED", "해당 레이어에는 완료된 AI 리소스가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        if (!isCompatible(layer.type(), resource.getResourceType())) {
            throw error("CARD_LAYER_RESOURCE_TYPE_MISMATCH", "레이어 유형과 AI 리소스 유형이 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> toLayerData(
            CardLayerRequest layer,
            AiResourceGeneration resource,
            Card card
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", layer.id());
        data.put("slot", layerSlot(layer));
        data.put("type", layer.type().name());
        data.put("sourceType", sourceType(layer, resource));
        data.put("resourceId", layer.resourceId());
        data.put("assetUrl", resource == null ? card.getProduct().getImageUrl() : resource.getGeneratedImageUrl());
        data.put("x", layer.x());
        data.put("y", layer.y());
        data.put("width", layer.width());
        data.put("height", layer.height());
        data.put("rotation", defaultValue(layer.rotation(), BigDecimal.ZERO));
        data.put("opacity", defaultValue(layer.opacity(), BigDecimal.ONE));
        data.put("zIndex", layer.zIndex() == null ? 1 : layer.zIndex());
        data.put("visible", layer.visible() == null || layer.visible());
        data.put("locked", layer.locked() != null && layer.locked());
        data.put("replaceable", true);
        data.put("isDefault", false);
        if (layer.text() != null) data.put("text", layer.text());
        if (layer.styleData() != null) data.put("styleData", layer.styleData());
        return data;
    }

    private String layerSlot(CardLayerRequest layer) {
        if (layer.slot() != null && !layer.slot().isBlank()) return layer.slot().toUpperCase();
        return layer.type().name();
    }

    private String sourceType(CardLayerRequest layer, AiResourceGeneration resource) {
        if (resource != null) return "AI";
        if (layer.type() == CardLayerType.PRODUCT) return "PRODUCT";
        return "USER";
    }

    private BigDecimal defaultValue(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private boolean isCompatible(CardLayerType layerType, com.cju.likelion.cardcollection.ai.domain.AiResourceType resourceType) {
        return switch (layerType) {
            case BACKGROUND -> resourceType == com.cju.likelion.cardcollection.ai.domain.AiResourceType.BACKGROUND;
            case PRODUCT -> false;
            case BORDER -> resourceType == com.cju.likelion.cardcollection.ai.domain.AiResourceType.BORDER;
            case PATTERN -> resourceType == com.cju.likelion.cardcollection.ai.domain.AiResourceType.PATTERN;
            case DECORATION -> resourceType == com.cju.likelion.cardcollection.ai.domain.AiResourceType.DECORATION;
            case FINISH -> true;
            case BASE_CARD, TEXT -> false;
        };
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }
}
