package com.cju.likelion.cardcollection.card.controller;

import com.cju.likelion.cardcollection.card.dto.PurchaseQrPreviewResponse;
import com.cju.likelion.cardcollection.card.service.CardService;
import com.cju.likelion.cardcollection.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-qrs")
public class PurchaseQrController {

    private final CardService cardService;

    public PurchaseQrController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<PurchaseQrPreviewResponse>> preview(
            @RequestParam @NotBlank String qrToken
    ) {
        return ResponseEntity.ok(new ApiResponse<>(cardService.previewPurchaseQr(qrToken)));
    }
}
