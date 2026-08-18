package com.cju.likelion.cardcollection.ai.dto;

import com.cju.likelion.cardcollection.card.dto.CardCustomizationResponse;
import com.cju.likelion.cardcollection.card.dto.CardResponse;

public record AiResourceCompositionResponse(
        CardResponse card,
        CardCustomizationResponse customization
) {
}
