package com.cju.likelion.cardcollection.local.controller;

import com.cju.likelion.cardcollection.common.api.ApiResponse;
import com.cju.likelion.cardcollection.local.service.LocalDemoResetService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// 시연 전 공동 테스트 기간에는 prod 프로필에서도 QR 복구가 필요하다.
// 발표 후에는 prod를 제거하거나 관리자/데모 키 인증으로 교체한다.
@Profile({"local", "test", "prod"})
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
