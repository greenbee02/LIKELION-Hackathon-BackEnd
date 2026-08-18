package com.cju.likelion.cardcollection.ai.provider;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;

public interface AiImageProvider {

    AiImageResult generate(AiResourceGeneration resource);
}
