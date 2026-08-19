package com.cju.likelion.cardcollection.ai.storage;

import com.cju.likelion.cardcollection.ai.provider.AiImageResult;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * AI가 생성한 이미지 리소스를 ISO/IEC 7810 ID-1 비율의 세로형 카드 캔버스로 맞춘다.
 * 표준 실물 카드 크기 85.60 x 53.98mm의 비율을 세로 방향으로 적용하면
 * 가로:세로가 약 1:1.586이 된다.
 */
@Component
public class CardAspectRatioImageNormalizer {

    public static final int CARD_WIDTH = 1000;
    public static final int CARD_HEIGHT = 1586;
    private static final double CARD_ASPECT_RATIO = (double) CARD_WIDTH / CARD_HEIGHT;

    public AiImageResult normalize(AiImageResult source) {
        if (source == null || source.imageBytes() == null || source.imageBytes().length == 0) {
            throw new IllegalArgumentException("AI 생성 이미지가 비어 있습니다.");
        }

        try {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source.imageBytes()));
            if (input == null) {
                throw new IOException("AI 생성 이미지를 읽을 수 없습니다.");
            }

            BufferedImage cropped = centerCrop(input);
            BufferedImage normalized = resize(cropped, CARD_WIDTH, CARD_HEIGHT, hasAlpha(input));
            String format = outputFormat(source.contentType());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(normalized, format, output)) {
                throw new IOException("이미지 형식으로 변환할 수 없습니다: " + format);
            }

            return AiImageResult.image(output.toByteArray(), "image/" + format, source.model());
        } catch (IOException exception) {
            throw new IllegalStateException("카드 표준 비율 이미지 변환에 실패했습니다.", exception);
        }
    }

    private BufferedImage centerCrop(BufferedImage input) {
        int sourceWidth = input.getWidth();
        int sourceHeight = input.getHeight();
        double sourceAspectRatio = (double) sourceWidth / sourceHeight;

        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        if (sourceAspectRatio > CARD_ASPECT_RATIO) {
            cropWidth = (int) Math.round(sourceHeight * CARD_ASPECT_RATIO);
        } else if (sourceAspectRatio < CARD_ASPECT_RATIO) {
            cropHeight = (int) Math.round(sourceWidth / CARD_ASPECT_RATIO);
        }

        int x = (sourceWidth - cropWidth) / 2;
        int y = (sourceHeight - cropHeight) / 2;
        return input.getSubimage(x, y, cropWidth, cropHeight);
    }

    private BufferedImage resize(BufferedImage input, int width, int height, boolean alpha) {
        int imageType = alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(width, height, imageType);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private boolean hasAlpha(BufferedImage image) {
        return image.getColorModel().hasAlpha();
    }

    private String outputFormat(String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        return "png";
    }
}
