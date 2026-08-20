package com.cju.likelion.cardcollection.card.controller;

import com.cju.likelion.cardcollection.card.dto.CardCustomizationResponse;
import com.cju.likelion.cardcollection.card.dto.CardCustomizationOptionsResponse;
import com.cju.likelion.cardcollection.card.dto.CardRegistrationRequest;
import com.cju.likelion.cardcollection.card.dto.CardResponse;
import com.cju.likelion.cardcollection.card.dto.CustomizationCreateRequest;
import com.cju.likelion.cardcollection.card.dto.LayeredCustomizationCreateRequest;
import com.cju.likelion.cardcollection.card.dto.LayeredCustomizationResponse;
import com.cju.likelion.cardcollection.card.service.CardService;
import com.cju.likelion.cardcollection.card.service.CardDesignAssetQueryService;
import com.cju.likelion.cardcollection.card.service.StaticCardCustomizationService;
import com.cju.likelion.cardcollection.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;
    private final CardDesignAssetQueryService designAssetQueryService;
    private final StaticCardCustomizationService staticCustomizationService;

    public CardController(
            CardService cardService,
            CardDesignAssetQueryService designAssetQueryService,
            StaticCardCustomizationService staticCustomizationService
    ) {
        this.cardService = cardService;
        this.designAssetQueryService = designAssetQueryService;
        this.staticCustomizationService = staticCustomizationService;
    }

    @PostMapping("/registrations")
    public ResponseEntity<ApiResponse<CardResponse>> register(
            Authentication authentication,
            @Valid @RequestBody CardRegistrationRequest request
    ) {
        CardResponse response = cardService.register(userId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> list(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(cardService.list(userId(authentication))));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardResponse>> get(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(cardService.get(userId(authentication), cardId)));
    }

    @GetMapping("/{cardId}/customization-options")
    public ResponseEntity<ApiResponse<CardCustomizationOptionsResponse>> customizationOptions(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(designAssetQueryService.get(userId(authentication), cardId)));
    }

    @PostMapping("/{cardId}/customizations")
    public ResponseEntity<ApiResponse<CardCustomizationResponse>> createCustomization(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody CustomizationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(cardService.createCustomization(userId(authentication), cardId, request)));
    }

    @PostMapping("/{cardId}/customizations/layers")
    public ResponseEntity<ApiResponse<LayeredCustomizationResponse>> createLayeredCustomization(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody LayeredCustomizationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                staticCustomizationService.create(userId(authentication), cardId, request)
        ));
    }

    @GetMapping("/{cardId}/customizations")
    public ResponseEntity<ApiResponse<List<CardCustomizationResponse>>> listCustomizations(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                cardService.listCustomizations(userId(authentication), cardId)));
    }

    @PostMapping("/{cardId}/customizations/{customizationId}/select")
    public ResponseEntity<ApiResponse<CardResponse>> selectCustomization(
            Authentication authentication,
            @PathVariable UUID cardId,
            @PathVariable UUID customizationId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                cardService.selectCustomization(userId(authentication), cardId, customizationId)));
    }

    @PostMapping("/{cardId}/restore-original")
    public ResponseEntity<ApiResponse<CardResponse>> restoreOriginal(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                cardService.restoreOriginal(userId(authentication), cardId)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
