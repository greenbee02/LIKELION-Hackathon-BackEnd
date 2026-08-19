package com.cju.likelion.cardcollection.ai.domain;

public enum AiResourceType {
    BACKGROUND,
    BORDER,
    PATTERN,
    /**
     * Legacy type kept so previously stored generations can still be read.
     * New PRODUCT_ANGLE generation requests are rejected by the service.
     */
    @Deprecated
    PRODUCT_ANGLE,
    DECORATION,
    COLOR_PALETTE,
    TEXT_STYLE,
    COMPOSITION
}
