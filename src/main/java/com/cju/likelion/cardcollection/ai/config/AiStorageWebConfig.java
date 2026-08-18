package com.cju.likelion.cardcollection.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class AiStorageWebConfig implements WebMvcConfigurer {

    private final Path storagePath;
    private final String publicBaseUrl;

    public AiStorageWebConfig(
            @Value("${app.ai.storage.path:build/generated-ai-resources}") String storagePath,
            @Value("${app.ai.storage.public-base-url:/generated/ai-resources}") String publicBaseUrl
    ) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "") + "/**";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicBaseUrl)
                .addResourceLocations(storagePath.toUri().toString());
    }
}
