package com.cju.likelion.cardcollection.ai.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LocalGeneratedImageStorage implements GeneratedImageStorage {

    private final Path storagePath;
    private final String publicBaseUrl;

    public LocalGeneratedImageStorage(
            @Value("${app.ai.storage.path:build/generated-ai-resources}") String storagePath,
            @Value("${app.ai.storage.public-base-url:/generated/ai-resources}") String publicBaseUrl
    ) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @Override
    public String store(UUID resourceId, byte[] imageBytes, String contentType) {
        String extension = extension(contentType);
        Path target = storagePath.resolve(resourceId + extension).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalStateException("이미지 저장 경로가 올바르지 않습니다.");
        }
        try {
            Files.createDirectories(storagePath);
            Files.write(target, imageBytes);
            return publicBaseUrl + "/" + target.getFileName();
        } catch (IOException exception) {
            throw new IllegalStateException("생성 이미지 저장에 실패했습니다.", exception);
        }
    }

    private String extension(String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType)) return ".jpg";
        if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
        return ".png";
    }
}
