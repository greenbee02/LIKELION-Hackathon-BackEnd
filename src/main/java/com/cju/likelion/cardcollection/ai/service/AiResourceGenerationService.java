package com.cju.likelion.cardcollection.ai.service;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceType;
import com.cju.likelion.cardcollection.ai.dto.AiResourceBatchGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationResponse;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCandidateGroupResponse;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationBatchResponse;
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
import java.util.Comparator;
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
    public AiResourceGenerationBatchResponse request(
            UUID userId,
            UUID cardId,
            AiResourceGenerationRequest request
    ) {
        Card card = findCard(userId, cardId);
        requireActive(card);
        rejectUnsupportedResourceType(request.resourceType());
        requireProductImage(request.resourceType(), card);
        UUID candidateGroupId = UUID.randomUUID();
        List<AiResourceGeneration> resources = createCandidates(card, request, candidateGroupId);
        return toBatchResponse(cardId, resourceRepository.saveAll(resources));
    }

    @Transactional
    public AiResourceGenerationBatchResponse requestBatch(
            UUID userId,
            UUID cardId,
            AiResourceBatchGenerationRequest batchRequest
    ) {
        Card card = findCard(userId, cardId);
        requireActive(card);

        Set<AiResourceType> resourceTypes = new java.util.HashSet<>();
        List<AiResourceGeneration> resources = new ArrayList<>();
        for (AiResourceGenerationRequest request : batchRequest.resources()) {
            if (!resourceTypes.add(request.resourceType())) {
                throw error(
                        "AI_RESOURCE_TYPE_DUPLICATED",
                        "같은 리소스 종류는 한 번의 추천 요청에 중복할 수 없습니다.",
                        HttpStatus.BAD_REQUEST
                );
            }
            rejectUnsupportedResourceType(request.resourceType());
            requireProductImage(request.resourceType(), card);
            resources.addAll(createCandidates(card, request, UUID.randomUUID()));
        }
        return toBatchResponse(cardId, resourceRepository.saveAll(resources));
    }

    private List<AiResourceGeneration> createCandidates(
            Card card,
            AiResourceGenerationRequest request,
            UUID candidateGroupId
    ) {
        int candidateCount = request.candidateCount() == null ? 4 : request.candidateCount();
        List<AiResourceGeneration> resources = new ArrayList<>();
        for (int candidateIndex = 1; candidateIndex <= candidateCount; candidateIndex++) {
            resources.add(createResource(card, request, candidateGroupId, candidateIndex, candidateCount));
        }
        return resources;
    }

    private AiResourceGeneration createResource(
            Card card,
            AiResourceGenerationRequest request,
            UUID candidateGroupId,
            int candidateIndex,
            int candidateCount
    ) {
        CardTemplate template = resolveTemplate(card, request.templateId());
        String sourceImageUrl = request.resourceType() == AiResourceType.BACKGROUND
                && card.getProduct() != null
                ? card.getProduct().getImageUrl()
                : null;
        return AiResourceGeneration.pending(
                card,
                card.getProduct(),
                template,
                request.resourceType(),
                request.prompt(),
                sourceImageUrl,
                serializeOptions(enrichOptions(request.options(), candidateIndex - 1, candidateCount)),
                candidateGroupId,
                candidateIndex,
                candidateCount,
                Instant.now()
        );
    }

    private void rejectUnsupportedResourceType(AiResourceType resourceType) {
        if (resourceType == AiResourceType.PRODUCT_ANGLE) {
            throw error(
                    "AI_RESOURCE_TYPE_UNSUPPORTED",
                    "PRODUCT_ANGLE은 AI 생성 대상에서 제외되었습니다. PRODUCT 레이어에서 상품 기본 이미지를 사용하세요.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void requireProductImage(AiResourceType resourceType, Card card) {
        if (resourceType != AiResourceType.BACKGROUND) return;

        if (card.getProduct() == null
                || card.getProduct().getImageUrl() == null
                || card.getProduct().getImageUrl().isBlank()) {
            throw error(
                    "PRODUCT_IMAGE_REQUIRED",
                    "상품 이미지가 등록된 카드만 상품 포함 배경을 생성할 수 있습니다.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private Map<String, Object> enrichOptions(Map<String, Object> options, int regionalVariant, int candidateCount) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (options != null) enriched.putAll(options);
        enriched.putIfAbsent("_regionalVariant", regionalVariant);
        enriched.put("_candidateIndex", regionalVariant + 1);
        enriched.put("_candidateCount", candidateCount);
        return enriched;
    }

    @Transactional(readOnly = true)
    public AiResourceGenerationBatchResponse list(UUID userId, UUID cardId) {
        findCard(userId, cardId);
        return toBatchResponse(cardId, resourceRepository.findByCardIdOrderByCreatedAtDesc(cardId));
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

    private AiResourceGenerationBatchResponse toBatchResponse(
            UUID cardId,
            List<AiResourceGeneration> resources
    ) {
        Map<UUID, List<AiResourceGeneration>> grouped = new LinkedHashMap<>();
        for (AiResourceGeneration resource : resources) {
            UUID key = resource.getCandidateGroupId() == null
                    ? resource.getId()
                    : resource.getCandidateGroupId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(resource);
        }

        List<AiResourceCandidateGroupResponse> groups = grouped.values().stream()
                .map(candidates -> {
                    List<AiResourceGenerationResponse> responses = candidates.stream()
                            .sorted(Comparator.comparing(
                                    AiResourceGeneration::getCandidateIndex,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(AiResourceGenerationResponse::from)
                            .toList();
                    AiResourceGeneration first = candidates.get(0);
                    int count = first.getCandidateCount() == null ? responses.size() : first.getCandidateCount();
                    return new AiResourceCandidateGroupResponse(
                            first.getCandidateGroupId(),
                            first.getResourceType().name(),
                            count,
                            responses
                    );
                })
                .toList();
        return new AiResourceGenerationBatchResponse(cardId, groups);
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
