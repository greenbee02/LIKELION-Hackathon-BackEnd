package com.cju.likelion.cardcollection.ai.provider;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OpenAiImageProvider implements AiImageProvider {

    private static final String CARD_CANVAS_INSTRUCTION =
            "Use a landscape ISO/IEC 7810 ID-1 card canvas with an 85.60:53.98 aspect ratio "
                    + "(approximately 1.586:1). Keep all important visual elements inside the card-safe area. ";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String size;
    private final String quality;
    private final String background;
    private final String outputFormat;

    public OpenAiImageProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.openai.model:gpt-image-1}") String model,
            @Value("${app.ai.openai.size:1024x1024}") String size,
            @Value("${app.ai.openai.quality:low}") String quality,
            @Value("${app.ai.openai.background:auto}") String background,
            @Value("${app.ai.openai.output-format:png}") String outputFormat
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.model = model;
        this.size = size;
        this.quality = quality;
        this.background = background;
        this.outputFormat = outputFormat;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiImageResult generate(AiResourceGeneration resource) {
        if (apiKey == null || apiKey.isBlank()) {
            throw AiProviderException.failed("OPENAI_API_KEY가 설정되지 않았습니다.", null);
        }

        try {
            String prompt = buildPrompt(resource);
            HttpResponse<String> response = resource.getSourceImageUrl() == null
                    ? sendGenerationRequest(prompt)
                    : sendEditRequest(prompt, resource.getSourceImageUrl());
            return parseImageResult(response);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw AiProviderException.failed("OpenAI 이미지 생성 요청이 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw AiProviderException.failed("OpenAI 이미지 생성 응답을 처리하지 못했습니다.", exception);
        }
    }

    private HttpResponse<String> sendGenerationRequest(String prompt)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("size", size);
        body.put("quality", quality);
        body.put("background", background);
        body.put("output_format", outputFormat);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/images/generations"))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return send(request);
    }

    private HttpResponse<String> sendEditRequest(String prompt, String sourceImageUrl)
            throws IOException, InterruptedException {
        byte[] sourceImage = downloadSourceImage(sourceImageUrl);
        String boundary = "----CodexAiBoundary" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, prompt, sourceImage);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/images/edits"))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = errorMessage(response.body());
            if (isPolicyRejection(response.statusCode(), message)) {
                throw AiProviderException.rejected(message);
            }
            throw AiProviderException.failed("OpenAI API 오류(" + response.statusCode() + "): " + message, null);
        }
        return response;
    }

    private AiImageResult parseImageResult(HttpResponse<String> response) throws IOException {
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode image = root.path("data").path(0);
        String base64 = image.path("b64_json").asText(null);
        if (base64 != null && !base64.isBlank()) {
            return new AiImageResult(Base64.getDecoder().decode(base64), "image/" + outputFormat, model);
        }

        String url = image.path("url").asText(null);
        if (url != null && !url.isBlank()) {
            try {
                return new AiImageResult(downloadSourceImage(url), "image/" + outputFormat, model);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("생성 결과 이미지 다운로드가 중단되었습니다.", exception);
            }
        }
        throw new IOException("OpenAI 응답에 생성 이미지가 없습니다.");
    }

    private byte[] downloadSourceImage(String sourceImageUrl) throws IOException, InterruptedException {
        URI uri;
        try {
            uri = URI.create(sourceImageUrl);
        } catch (IllegalArgumentException exception) {
            throw new IOException("sourceImageUrl 형식이 올바르지 않습니다.", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("상품 원본 이미지는 외부에서 접근 가능한 HTTP(S) URL이어야 합니다.");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("원본 이미지 다운로드 실패: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private byte[] multipartBody(String boundary, String prompt, byte[] image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writePart(output, boundary, "model", model);
        writePart(output, boundary, "prompt", prompt);
        writePart(output, boundary, "size", size);
        writePart(output, boundary, "quality", quality);
        writePart(output, boundary, "background", background);
        writePart(output, boundary, "output_format", outputFormat);
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write("Content-Disposition: form-data; name=\"image\"; filename=\"source.png\"\r\n".getBytes(StandardCharsets.UTF_8));
        output.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(image);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writePart(ByteArrayOutputStream output, String boundary, String name, String value)
            throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String buildPrompt(AiResourceGeneration resource) {
        String typeInstruction = switch (resource.getResourceType()) {
            case BACKGROUND -> "Create a reusable card background asset";
            case BORDER -> "Create a reusable card border asset with transparent center";
            case PATTERN -> "Create a reusable decorative card pattern asset";
            case PRODUCT_ANGLE -> "Create a faithful product image from the requested viewing angle";
            case DECORATION -> "Create a reusable decorative card element";
            case COLOR_PALETTE -> "Create a visual color palette asset for the card";
            case TEXT_STYLE -> "Create a visual typography style asset for the card";
            case COMPOSITION -> "Create a reusable card composition asset";
        };
        String prompt = resource.getPrompt() == null ? "" : resource.getPrompt();
        String options = resource.getGeneratedData() == null ? "" :
                " Design options JSON: " + resource.getGeneratedData();
        return CARD_CANVAS_INSTRUCTION + typeInstruction
                + ". Do not create a complete branded card, logo, or unrelated text. "
                + prompt + options;
    }

    private String errorMessage(String body) {
        try {
            JsonNode error = objectMapper.readTree(body).path("error").path("message");
            if (!error.isMissingNode()) return error.asText();
        } catch (IOException ignored) {
            // Fall back to the raw response when the provider returns non-JSON.
        }
        return body == null || body.isBlank() ? "응답 본문이 없습니다." : body;
    }

    private boolean isPolicyRejection(int statusCode, String message) {
        String lower = message.toLowerCase();
        return statusCode == 400 && (lower.contains("safety")
                || lower.contains("policy")
                || lower.contains("moderation")
                || lower.contains("content_policy")
                || lower.contains("rejected"));
    }
}
