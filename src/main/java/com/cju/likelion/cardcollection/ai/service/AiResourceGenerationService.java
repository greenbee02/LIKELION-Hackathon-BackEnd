package com.cju.likelion.cardcollection.ai.service;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceType;
import com.cju.likelion.cardcollection.ai.dto.AiResourceBatchGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationResponse;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.repository.CardTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiResourceGenerationService {

    private final CardRepository cardRepository;
    private final CardTemplateRepository templateRepository;
    private final AiResourceGenerationRepository resourceRepository;
    private final ObjectMapper objectMapper;

    public AiResourceGenerationService(
            CardRepository cardRepository,
            CardTemplateRepository templateRepository,
            AiResourceGenerationRepository resourceRepository,
            ObjectMapper objectMapper
    ) {
        this.cardRepository = cardRepository;
        this.templateRepository = templateRepository;
        this.resourceRepository = resourceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiResourceGenerationResponse request(
            UUID userId,
            UUID cardId,
            AiResourceGenerationRequest request
    ) {
        Card card = findCard(userId, cardId);
        requireActive(card);
        AiResourceGeneration resource = createResource(card, request, nextRegionalVariant(card.getId(), 0));
        return AiResourceGenerationResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public List<AiResourceGenerationResponse> requestBatch(
            UUID userId,
            UUID cardId,
            AiResourceBatchGenerationRequest batchRequest
    ) {
        Card card = findCard(userId, cardId);
        requireActive(card);

        int startingVariant = nextRegionalVariant(cardId, 0);
        List<AiResourceGenerationResponse> responses = new ArrayList<>();
        for (int index = 0; index < batchRequest.resources().size(); index++) {
            AiResourceGeneration resource = createResource(
                    card,
                    batchRequest.resources().get(index),
                    startingVariant + index
            );
            responses.add(AiResourceGenerationResponse.from(resourceRepository.save(resource)));
        }
        return responses;
    }

    private AiResourceGeneration createResource(
            Card card,
            AiResourceGenerationRequest request,
            int regionalVariant
    ) {
        CardTemplate template = resolveTemplate(card, request.templateId());
        String sourceImageUrl = request.sourceImageUrl() != null
                ? request.sourceImageUrl()
                : card.getProduct().getImageUrl();

        if (request.resourceType() == AiResourceType.PRODUCT_ANGLE && sourceImageUrl == null) {
            throw error(
                    "AI_SOURCE_IMAGE_REQUIRED",
                    "상품 각도 이미지 생성에는 원본 상품 이미지가 필요합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        return AiResourceGeneration.pending(
                card,
                card.getProduct(),
                template,
                request.resourceType(),
                request.prompt(),
                sourceImageUrl,
                serializeOptions(enrichOptions(request.options(), regionalVariant)),
                Instant.now()
        );
    }

    private Map<String, Object> enrichOptions(Map<String, Object> options, int regionalVariant) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (options != null) enriched.putAll(options);
        enriched.putIfAbsent("_regionalVariant", regionalVariant);
        return enriched;
    }

    private int nextRegionalVariant(UUID cardId, int offset) {
        long existing = resourceRepository.findByCardIdOrderByCreatedAtDesc(cardId).stream()
                .filter(resource -> isRegionalResource(resource.getResourceType()))
                .count();
        return Math.toIntExact(existing) + offset;
    }

    private boolean isRegionalResource(AiResourceType resourceType) {
        return Set.of(
                AiResourceType.BACKGROUND,
                AiResourceType.PATTERN,
                AiResourceType.DECORATION,
                AiResourceType.COMPOSITION
        ).contains(resourceType);
    }

    @Transactional(readOnly = true)
    public List<AiResourceGenerationResponse> list(UUID userId, UUID cardId) {
        findCard(userId, cardId);
        return resourceRepository.findByCardIdOrderByCreatedAtDesc(cardId).stream()
                .map(AiResourceGenerationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiResourceGenerationResponse get(UUID userId, UUID cardId, UUID resourceId) {
        findCard(userId, cardId);
        AiResourceGeneration resource = resourceRepository.findByIdAndCardId(resourceId, cardId)
                .orElseThrow(() -> error(
                        "AI_RESOURCE_NOT_FOUND",
                        "AI 리소스 생성 이력을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
        return AiResourceGenerationResponse.from(resource);
    }

    private Card findCard(UUID userId, UUID cardId) {
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private CardTemplate resolveTemplate(Card card, UUID templateId) {
        if (templateId == null) return card.getTemplate();
        CardTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> error(
                        "TEMPLATE_NOT_FOUND",
                        "카드 템플릿을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
        if (!template.isActive()) {
            throw error("TEMPLATE_INACTIVE", "비활성화된 카드 템플릿입니다.", HttpStatus.CONFLICT);
        }
        if (!template.getBrand().getId().equals(card.getProduct().getBrand().getId())) {
            throw error("TEMPLATE_BRAND_MISMATCH", "카드 상품과 같은 브랜드의 템플릿만 사용할 수 있습니다.", HttpStatus.CONFLICT);
        }
        if (template.getAllowedCardType() != null
                && template.getAllowedCardType() != card.getOriginalCardType()) {
            throw error(
                    "TEMPLATE_CARD_TYPE_NOT_ALLOWED",
                    "현재 카드 타입에 사용할 수 없는 템플릿입니다.",
                    HttpStatus.CONFLICT
            );
        }
        return template;
    }

    private String serializeOptions(java.util.Map<String, Object> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException exception) {
            throw error("AI_RESOURCE_OPTIONS_INVALID", "AI 리소스 옵션을 저장할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }

    private void requireActive(Card card) {
        if (card.getStatus() != com.cju.likelion.cardcollection.card.domain.CardStatus.ACTIVE) {
            throw error("CARD_NOT_ACTIVE", "활성 상태의 카드만 AI 리소스를 생성할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }
}
