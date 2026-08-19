package com.cju.likelion.cardcollection.ai.storage;

import com.cju.likelion.cardcollection.ai.provider.AiImageResult;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardAspectRatioImageNormalizerTest {

    private final CardAspectRatioImageNormalizer normalizer = new CardAspectRatioImageNormalizer();

    @Test
    void generatedImageIsNormalizedToPortraitId1CardRatio() throws Exception {
        BufferedImage input = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);

        AiImageResult result = normalizer.normalize(
                AiImageResult.image(bytes.toByteArray(), "image/png", "test"));
        BufferedImage output = ImageIO.read(new ByteArrayInputStream(result.imageBytes()));

        assertEquals(1000, output.getWidth());
        assertEquals(1586, output.getHeight());
        assertEquals(CardAspectRatioImageNormalizer.CARD_WIDTH, output.getWidth());
        assertEquals(CardAspectRatioImageNormalizer.CARD_HEIGHT, output.getHeight());
        assertEquals("image/png", result.contentType());
    }
}
