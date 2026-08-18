package com.cju.likelion.cardcollection.ai.service;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionResponse;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.card.domain.CardStatus;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        String customizationData = serializeComposition(resources, request.layoutData());
        CardTemplate template = card.getTemplate();
        CardCustomization customization = CardCustomization.composed(
                card,
                template,
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
            List<AiResourceGeneration> resources,
            Map<String, Object> layoutData
    ) {
        Map<String, Object> composition = new LinkedHashMap<>();
        composition.put("version", "composition-v1");
        composition.put("resourceIds", resources.stream().map(AiResourceGeneration::getId).toList());
        composition.put("resources", resources.stream().map(resource -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", resource.getId());
            item.put("type", resource.getResourceType().name());
            item.put("generatedImageUrl", resource.getGeneratedImageUrl());
            item.put("generatedData", resource.getGeneratedData());
            return item;
        }).toList());
        composition.put("layoutData", layoutData);
        try {
            return objectMapper.writeValueAsString(composition);
        } catch (JsonProcessingException exception) {
            throw error("AI_COMPOSITION_INVALID", "AI 리소스 조합 데이터를 저장할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }
}
