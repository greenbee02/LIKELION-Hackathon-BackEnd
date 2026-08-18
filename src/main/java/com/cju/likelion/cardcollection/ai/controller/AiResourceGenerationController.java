package com.cju.likelion.cardcollection.ai.controller;

import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceGenerationResponse;
import com.cju.likelion.cardcollection.ai.dto.AiResourceBatchGenerationRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionRequest;
import com.cju.likelion.cardcollection.ai.dto.AiResourceCompositionResponse;
import com.cju.likelion.cardcollection.ai.service.AiResourceCompositionService;
import com.cju.likelion.cardcollection.ai.service.AiResourceGenerationService;
import com.cju.likelion.cardcollection.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards/{cardId}/ai-resources")
public class AiResourceGenerationController {

    private final AiResourceGenerationService service;
    private final AiResourceCompositionService compositionService;

    public AiResourceGenerationController(
            AiResourceGenerationService service,
            AiResourceCompositionService compositionService
    ) {
        this.service = service;
        this.compositionService = compositionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiResourceGenerationResponse>> request(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody AiResourceGenerationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(
                service.request(userId(authentication), cardId, request)
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<AiResourceGenerationResponse>>> requestBatch(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody AiResourceBatchGenerationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(
                service.requestBatch(userId(authentication), cardId, request)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiResourceGenerationResponse>>> list(
            Authentication authentication,
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(service.list(userId(authentication), cardId)));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<AiResourceGenerationResponse>> get(
            Authentication authentication,
            @PathVariable UUID cardId,
            @PathVariable UUID resourceId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                service.get(userId(authentication), cardId, resourceId)
        ));
    }

    @PostMapping("/compose")
    public ResponseEntity<ApiResponse<AiResourceCompositionResponse>> compose(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody AiResourceCompositionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                compositionService.compose(userId(authentication), cardId, request)
        ));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
