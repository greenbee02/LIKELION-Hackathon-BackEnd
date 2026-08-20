package com.cju.likelion.cardcollection.card.service;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.auth.exception.InvalidCredentialsException;
import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.card.domain.CardType;
import com.cju.likelion.cardcollection.card.domain.PurchaseQr;
import com.cju.likelion.cardcollection.card.dto.CardCustomizationResponse;
import com.cju.likelion.cardcollection.card.dto.CardRegistrationRequest;
import com.cju.likelion.cardcollection.card.dto.CardResponse;
import com.cju.likelion.cardcollection.card.dto.CustomizationCreateRequest;
import com.cju.likelion.cardcollection.card.dto.PurchaseQrPreviewResponse;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import com.cju.likelion.cardcollection.card.repository.CardCustomizationRepository;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.card.repository.PurchaseQrRepository;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.repository.CardTemplateRepository;
import com.cju.likelion.cardcollection.reward.service.RewardUnlockService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private static final String DEFAULT_BASIC_FRONT_IMAGE = "/images/templates/border_03.png";
    private static final String DEFAULT_BASIC_BACK_IMAGE = "/images/templates/common_back_black_info.png";

    private final UserRepository userRepository;
    private final PurchaseQrRepository purchaseQrRepository;
    private final CardRepository cardRepository;
    private final CardCustomizationRepository customizationRepository;
    private final CardTemplateRepository templateRepository;
    private final RewardUnlockService rewardUnlockService;

    public CardService(
            UserRepository userRepository,
            PurchaseQrRepository purchaseQrRepository,
            CardRepository cardRepository,
            CardCustomizationRepository customizationRepository,
            CardTemplateRepository templateRepository,
            RewardUnlockService rewardUnlockService
    ) {
        this.userRepository = userRepository;
        this.purchaseQrRepository = purchaseQrRepository;
        this.cardRepository = cardRepository;
        this.customizationRepository = customizationRepository;
        this.templateRepository = templateRepository;
        this.rewardUnlockService = rewardUnlockService;
    }

    @Transactional
    public CardResponse register(UUID userId, CardRegistrationRequest request) {
        User user = activeUser(userId);
        PurchaseQr qr = purchaseQrRepository.findByQrTokenForUpdate(request.qrToken().trim())
                .orElseThrow(() -> error("QR_TOKEN_INVALID", "유효하지 않은 구매 QR입니다.", HttpStatus.NOT_FOUND));

        if (qr.isUsed()) {
            throw error("QR_ALREADY_USED", "이미 사용된 구매 QR입니다.", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        if (qr.isExpired(now)) {
            throw error("QR_EXPIRED", "만료된 구매 QR입니다.", HttpStatus.CONFLICT);
        }

        CardType originalType = qr.getProduct().isLimited() ? CardType.COLLECTOR : CardType.BASIC;
        if (!qr.getProduct().isActive()) {
            throw error("PRODUCT_INACTIVE", "비활성화된 상품 또는 경험은 카드를 발급할 수 없습니다.", HttpStatus.CONFLICT);
        }
        CardTemplate template = findTemplate(originalType, qr.getProduct().getBrand().getId());
        Card card = cardRepository.save(Card.issue(user, qr, template, now));
        qr.markUsed(user, now);
        rewardUnlockService.evaluate(user);
        return CardResponse.from(card);
    }

    @Transactional(readOnly = true)
    public PurchaseQrPreviewResponse previewPurchaseQr(String qrToken) {
        PurchaseQr qr = purchaseQrRepository.findByQrToken(qrToken.trim())
                .orElseThrow(() -> error("QR_TOKEN_INVALID", "유효하지 않은 구매 QR입니다.", HttpStatus.NOT_FOUND));
        return PurchaseQrPreviewResponse.from(qr, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list(UUID userId) {
        activeUser(userId);
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse get(UUID userId, UUID cardId) {
        return CardResponse.from(findCard(userId, cardId));
    }

    @Transactional
    public CardCustomizationResponse createCustomization(
            UUID userId,
            UUID cardId,
            CustomizationCreateRequest request
    ) {
        Card card = findCard(userId, cardId);
        requireActive(card);
        CardTemplate template = request.templateId() == null
                ? card.getTemplate()
                : templateRepository.findById(request.templateId())
                .orElseThrow(() -> error("TEMPLATE_NOT_FOUND", "카드 템플릿을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!template.isActive()) {
            throw error("TEMPLATE_INACTIVE", "비활성화된 카드 템플릿입니다.", HttpStatus.CONFLICT);
        }
        requireSameBrand(card, template);
        if (template.getAllowedCardType() != null
                && template.getAllowedCardType() != card.getOriginalCardType()) {
            throw error("TEMPLATE_CARD_TYPE_NOT_ALLOWED", "현재 카드 타입에 사용할 수 없는 템플릿입니다.", HttpStatus.CONFLICT);
        }

        CardCustomization customization = CardCustomization.completed(
                card,
                template,
                request.inputImageUrl(),
                request.inputText(),
                request.inputText(),
                Instant.now()
        );
        return CardCustomizationResponse.from(customizationRepository.save(customization));
    }

    @Transactional(readOnly = true)
    public List<CardCustomizationResponse> listCustomizations(UUID userId, UUID cardId) {
        findCard(userId, cardId);
        return customizationRepository.findByCardIdOrderByCreatedAtDesc(cardId).stream()
                .map(CardCustomizationResponse::from)
                .toList();
    }

    @Transactional
    public CardResponse selectCustomization(UUID userId, UUID cardId, UUID customizationId) {
        Card card = findCard(userId, cardId);
        requireActive(card);
        CardCustomization customization = customizationRepository.findByIdAndCardId(customizationId, cardId)
                .orElseThrow(() -> error("CUSTOMIZATION_NOT_FOUND", "커스터마이징 이력을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!customization.isCompleted()) {
            throw error("CUSTOMIZATION_NOT_COMPLETED", "완료된 커스터마이징만 선택할 수 있습니다.", HttpStatus.CONFLICT);
        }
        card.selectCustomization(customization);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse restoreOriginal(UUID userId, UUID cardId) {
        Card card = findCard(userId, cardId);
        requireActive(card);
        card.restoreOriginal();
        return CardResponse.from(card);
    }

    private User activeUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private Card findCard(UUID userId, UUID cardId) {
        activeUser(userId);
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private CardTemplate findTemplate(CardType type, UUID brandId) {
        List<CardTemplate> candidates = templateRepository.findAllByActiveTrueOrderByCreatedAtAsc().stream()
                .filter(template -> template.getBrand().getId().equals(brandId))
                .filter(template -> template.getAllowedCardType() == null || template.getAllowedCardType() == type)
                .toList();

        if (type == CardType.BASIC) {
            return candidates.stream()
                    .filter(template -> DEFAULT_BASIC_FRONT_IMAGE.equals(template.getFrontImageUrl()))
                    .filter(template -> DEFAULT_BASIC_BACK_IMAGE.equals(template.getBackImageUrl()))
                    .findFirst()
                    .orElseGet(() -> candidates.stream().findFirst()
                            .orElseThrow(() -> error(
                                    "CARD_TEMPLATE_NOT_FOUND",
                                    "발급 가능한 카드 템플릿이 없습니다.",
                                    HttpStatus.CONFLICT
                            )));
        }

        return candidates.stream().findFirst()
                .orElseThrow(() -> error("CARD_TEMPLATE_NOT_FOUND", "발급 가능한 카드 템플릿이 없습니다.", HttpStatus.CONFLICT));
    }

    private CardDomainException error(String code, String message, HttpStatus status) {
        return new CardDomainException(code, message, status);
    }

    private void requireActive(Card card) {
        if (card.getStatus() != com.cju.likelion.cardcollection.card.domain.CardStatus.ACTIVE) {
            throw error("CARD_NOT_ACTIVE", "활성 상태의 카드만 변경할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }

    private void requireSameBrand(Card card, CardTemplate template) {
        if (!card.getProduct().getBrand().getId().equals(template.getBrand().getId())) {
            throw error("TEMPLATE_BRAND_MISMATCH", "카드 상품과 같은 브랜드의 템플릿만 사용할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }
}
