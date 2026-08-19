package com.cju.likelion.cardcollection.local.controller;

import com.cju.likelion.cardcollection.common.api.ApiResponse;
import com.cju.likelion.cardcollection.local.service.LocalDemoResetService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/v1/local/demo")
public class LocalDemoController {

    private final LocalDemoResetService resetService;

    public LocalDemoController(LocalDemoResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<DemoResetResponse>> reset() {
        int resetPurchaseQrCount = resetService.reset();
        return ResponseEntity.ok(new ApiResponse<>(new DemoResetResponse(resetPurchaseQrCount)));
    }

    public record DemoResetResponse(int resetPurchaseQrCount) {
    }
}
