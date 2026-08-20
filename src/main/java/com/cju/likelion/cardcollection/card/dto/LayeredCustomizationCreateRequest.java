package com.cju.likelion.cardcollection.card.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record LayeredCustomizationCreateRequest(
        @NotNull UUID productBackgroundAssetId,
        @NotNull UUID borderAssetId,
        @Valid @NotNull TextLayerRequest text,
        @NotNull UUID backLayoutId
) {
    public record TextLayerRequest(
            @NotBlank String content,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal x,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal y,
            @NotNull @DecimalMin(value = "0.000001") @DecimalMax("1.0") BigDecimal width,
            @NotNull @DecimalMin(value = "0.000001") @DecimalMax("1.0") BigDecimal height,
            @NotNull @DecimalMin("-360.0") @DecimalMax("360.0") BigDecimal rotation,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal opacity,
            @NotNull @Min(0) Integer zIndex,
            Map<String, Object> style
    ) {
    }
}
