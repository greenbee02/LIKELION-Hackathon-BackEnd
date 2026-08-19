package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.card.domain.CardLayerType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CardLayerRequest(
        @NotBlank(message = "레이어 id는 필수입니다.")
        @Size(max = 100, message = "레이어 id는 100자 이하여야 합니다.")
        String id,
        @NotNull(message = "레이어 type은 필수입니다.")
        CardLayerType type,
        @Size(max = 50, message = "레이어 slot은 50자 이하여야 합니다.")
        String slot,
        UUID resourceId,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal x,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal y,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal width,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal height,
        @DecimalMin(value = "-360.0") @DecimalMax(value = "360.0") BigDecimal rotation,
        @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal opacity,
        Integer zIndex,
        Boolean visible,
        Boolean locked,
        @Size(max = 2000, message = "텍스트는 2000자 이하여야 합니다.") String text,
        Map<String, Object> styleData
) {
}
