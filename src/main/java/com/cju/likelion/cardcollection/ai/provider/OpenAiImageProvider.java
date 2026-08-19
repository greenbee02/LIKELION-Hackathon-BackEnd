package com.cju.likelion.cardcollection.ai.provider;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceType;
import com.cju.likelion.cardcollection.catalog.domain.Store;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class OpenAiImageProvider implements AiImageProvider {

    private static final String CARD_CANVAS_INSTRUCTION =
            "Use a portrait ISO/IEC 7810 ID-1 card canvas with a 53.98:85.60 aspect ratio "
                    + "(approximately 1:1.586, width:height). Keep all important visual elements inside the card-safe area. ";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String textModel;
    private final String size;
    private final String quality;
    private final String background;
    private final String outputFormat;

    public OpenAiImageProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.openai.model:gpt-image-1}") String model,
            @Value("${app.ai.openai.text-model:gpt-5-mini}") String textModel,
            @Value("${app.ai.openai.size:1024x1024}") String size,
            @Value("${app.ai.openai.quality:low}") String quality,
            @Value("${app.ai.openai.background:auto}") String background,
            @Value("${app.ai.openai.output-format:png}") String outputFormat
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.model = model;
        this.textModel = textModel;
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
            if (usesStructuredData(resource.getResourceType())) {
                return generateStructuredData(resource);
            }

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
            return AiImageResult.image(Base64.getDecoder().decode(base64), "image/" + outputFormat, model);
        }

        String url = image.path("url").asText(null);
        if (url != null && !url.isBlank()) {
            try {
                return AiImageResult.image(downloadSourceImage(url), "image/" + outputFormat, model);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("생성 결과 이미지 다운로드가 중단되었습니다.", exception);
            }
        }
        throw new IOException("OpenAI 응답에 생성 이미지가 없습니다.");
    }

    private AiImageResult generateColorPalette(AiResourceGeneration resource)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", textModel);
        body.put("input", colorPalettePrompt(resource));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "color_palette",
                "strict", true,
                "schema", colorPaletteSchema()
        )));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/responses"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = send(request);
        JsonNode root = objectMapper.readTree(response.body());
        String generatedData = responseOutputText(root);
        validateColorPalette(generatedData);
        return AiImageResult.data(generatedData, textModel);
    }

    private AiImageResult generateStructuredData(AiResourceGeneration resource)
            throws IOException, InterruptedException {
        AiResourceType resourceType = resource.getResourceType();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", textModel);
        body.put("input", structuredDataPrompt(resource));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", resourceType.name().toLowerCase(Locale.ROOT),
                "strict", true,
                "schema", structuredDataSchema(resourceType)
        )));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/responses"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = send(request);
        JsonNode root = objectMapper.readTree(response.body());
        String generatedData = responseOutputText(root);
        validateStructuredData(resourceType, generatedData);
        return AiImageResult.data(generatedData, textModel);
    }

    private boolean usesStructuredData(AiResourceType resourceType) {
        return resourceType == AiResourceType.COLOR_PALETTE
                || resourceType == AiResourceType.TEXT_STYLE
                || resourceType == AiResourceType.COMPOSITION;
    }

    private String structuredDataPrompt(AiResourceGeneration resource) {
        String prompt = resource.getPrompt() == null ? "" : resource.getPrompt().trim();
        String options = resource.getGeneratedData() == null ? "{}" : resource.getGeneratedData();
        String productName = resource.getProduct() == null || resource.getProduct().getName() == null
                ? ""
                : resource.getProduct().getName();
        return switch (resource.getResourceType()) {
            case COLOR_PALETTE -> colorPalettePrompt(resource);
            case TEXT_STYLE -> "Recommend a practical typography style for a collectible product card. "
                    + "Return only the JSON object required by the schema. "
                    + "Use CSS-compatible font values, a numeric font weight from 100 to 900, and normalized 0 to 1 coordinates. "
                    + "Choose a readable text color and strong contrast. "
                    + candidateInstruction(resource)
                    + "Product: " + productName + ". User request: " + prompt + ". Design options JSON: " + options;
            case COMPOSITION -> "Recommend a reusable layout for a collectible product card. "
                    + "Return only the JSON object required by the schema. "
                    + "Use a 1000x1586 portrait canvas and normalized 0 to 1 coordinates for every layer. "
                    + "Keep layers inside the card-safe area, use a small number of useful layers, and do not include readable text content. "
                    + candidateInstruction(resource)
                    + "Product: " + productName + ". User request: " + prompt + ". Design options JSON: " + options;
            default -> throw new IllegalArgumentException("구조화 데이터가 지원되지 않는 리소스 유형입니다.");
        };
    }

    private Map<String, Object> structuredDataSchema(AiResourceType resourceType) {
        return switch (resourceType) {
            case COLOR_PALETTE -> colorPaletteSchema();
            case TEXT_STYLE -> textStyleSchema();
            case COMPOSITION -> compositionSchema();
            default -> throw new IllegalArgumentException("구조화 데이터가 지원되지 않는 리소스 유형입니다.");
        };
    }

    private Map<String, Object> textStyleSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("styleName", Map.of("type", "string"));
        properties.put("fontFamily", Map.of("type", "string"));
        properties.put("fontWeight", Map.of("type", "integer"));
        properties.put("fontSize", Map.of("type", "number"));
        properties.put("letterSpacing", Map.of("type", "number"));
        properties.put("lineHeight", Map.of("type", "number"));
        properties.put("color", Map.of("type", "string"));
        properties.put("textAlign", Map.of("type", "string", "enum", List.of("left", "center", "right")));
        properties.put("maxLines", Map.of("type", "integer"));
        properties.put("x", Map.of("type", "number"));
        properties.put("y", Map.of("type", "number"));
        properties.put("width", Map.of("type", "number"));
        properties.put("height", Map.of("type", "number"));
        properties.put("rationale", Map.of("type", "string"));
        return objectSchema(properties, List.of(
                "styleName", "fontFamily", "fontWeight", "fontSize", "letterSpacing", "lineHeight",
                "color", "textAlign", "maxLines", "x", "y", "width", "height", "rationale"));
    }

    private Map<String, Object> compositionSchema() {
        Map<String, Object> layerProperties = new LinkedHashMap<>();
        layerProperties.put("id", Map.of("type", "string"));
        layerProperties.put("type", Map.of("type", "string", "enum",
                List.of("BACKGROUND", "PRODUCT", "TEXT", "BORDER", "PATTERN", "DECORATION")));
        layerProperties.put("x", Map.of("type", "number"));
        layerProperties.put("y", Map.of("type", "number"));
        layerProperties.put("width", Map.of("type", "number"));
        layerProperties.put("height", Map.of("type", "number"));
        layerProperties.put("rotation", Map.of("type", "number"));
        layerProperties.put("opacity", Map.of("type", "number"));
        layerProperties.put("zIndex", Map.of("type", "integer"));
        layerProperties.put("visible", Map.of("type", "boolean"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("layoutName", Map.of("type", "string"));
        properties.put("canvasWidth", Map.of("type", "integer"));
        properties.put("canvasHeight", Map.of("type", "integer"));
        properties.put("backgroundColor", Map.of("type", "string"));
        properties.put("layers", Map.of(
                "type", "array",
                "minItems", 1,
                "maxItems", 8,
                "items", objectSchema(layerProperties, List.of(
                        "id", "type", "x", "y", "width", "height", "rotation", "opacity", "zIndex", "visible"))));
        properties.put("rationale", Map.of("type", "string"));
        return objectSchema(properties, List.of(
                "layoutName", "canvasWidth", "canvasHeight", "backgroundColor", "layers", "rationale"));
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required
    ) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private String colorPalettePrompt(AiResourceGeneration resource) {
        String prompt = resource.getPrompt() == null ? "" : resource.getPrompt().trim();
        String options = resource.getGeneratedData() == null ? "{}" : resource.getGeneratedData();
        String productName = resource.getProduct() == null || resource.getProduct().getName() == null
                ? ""
                : resource.getProduct().getName();
        return "Recommend a practical color palette for a collectible product card. "
                + "Return only the JSON object required by the schema. "
                + "Every color must be a six-digit uppercase hexadecimal value such as #1A2B3C. "
                + "Ensure text has strong contrast against background. "
                + candidateInstruction(resource)
                + "Product: " + productName + ". "
                + "User request: " + prompt + ". "
                + "Design options JSON: " + options;
    }

    private Map<String, Object> colorPaletteSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("paletteName", Map.of("type", "string"));
        properties.put("primary", Map.of("type", "string"));
        properties.put("secondary", Map.of("type", "string"));
        properties.put("accent", Map.of("type", "string"));
        properties.put("background", Map.of("type", "string"));
        properties.put("text", Map.of("type", "string"));
        properties.put("rationale", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of(
                "paletteName", "primary", "secondary", "accent", "background", "text", "rationale"));
        return schema;
    }

    private String responseOutputText(JsonNode root) throws IOException {
        String outputText = root.path("output_text").asText(null);
        if (outputText != null && !outputText.isBlank()) return outputText;

        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                String text = content.path("text").asText(null);
                if (text != null && !text.isBlank()) return text;
            }
        }
        throw new IOException("OpenAI 응답에 구조화 JSON이 없습니다.");
    }

    private void validateColorPalette(String generatedData) throws IOException {
        JsonNode palette = objectMapper.readTree(generatedData);
        for (String field : List.of("primary", "secondary", "accent", "background", "text")) {
            String color = palette.path(field).asText("");
            if (!color.matches("#[0-9A-Fa-f]{6}")) {
                throw new IOException("OpenAI가 올바른 HEX 색상을 반환하지 않았습니다: " + field);
            }
        }
    }

    private void validateStructuredData(AiResourceType resourceType, String generatedData) throws IOException {
        switch (resourceType) {
            case COLOR_PALETTE -> validateColorPalette(generatedData);
            case TEXT_STYLE -> validateTextStyle(generatedData);
            case COMPOSITION -> validateComposition(generatedData);
            default -> throw new IOException("지원되지 않는 구조화 리소스 유형입니다.");
        }
    }

    private void validateTextStyle(String generatedData) throws IOException {
        JsonNode style = objectMapper.readTree(generatedData);
        int fontWeight = style.path("fontWeight").asInt(0);
        double fontSize = style.path("fontSize").asDouble(-1);
        double lineHeight = style.path("lineHeight").asDouble(-1);
        int maxLines = style.path("maxLines").asInt(0);
        if (fontWeight < 100 || fontWeight > 900 || fontSize <= 0 || fontSize > 200
                || lineHeight <= 0 || lineHeight > 5 || maxLines < 1 || maxLines > 10) {
            throw new IOException("OpenAI가 올바른 텍스트 스타일 값을 반환하지 않았습니다.");
        }
        validateHexColor(style.path("color").asText(""), "color");
        validateNormalizedBounds(style, "텍스트 스타일");
    }

    private void validateComposition(String generatedData) throws IOException {
        JsonNode composition = objectMapper.readTree(generatedData);
        if (composition.path("canvasWidth").asInt(0) != 1000
                || composition.path("canvasHeight").asInt(0) != 1586) {
            throw new IOException("Composition canvas는 1000x1586이어야 합니다.");
        }
        validateHexColor(composition.path("backgroundColor").asText(""), "backgroundColor");
        JsonNode layers = composition.path("layers");
        if (!layers.isArray() || layers.isEmpty() || layers.size() > 8) {
            throw new IOException("OpenAI가 올바른 composition layer를 반환하지 않았습니다.");
        }
        for (JsonNode layer : layers) {
            validateNormalizedBounds(layer, "composition layer");
            double rotation = layer.path("rotation").asDouble(999);
            double opacity = layer.path("opacity").asDouble(-1);
            if (rotation < -360 || rotation > 360 || opacity < 0 || opacity > 1) {
                throw new IOException("OpenAI가 올바른 composition 변환 값을 반환하지 않았습니다.");
            }
        }
    }

    private void validateHexColor(String value, String field) throws IOException {
        if (!value.matches("#[0-9A-Fa-f]{6}")) {
            throw new IOException("OpenAI가 올바른 HEX 색상을 반환하지 않았습니다: " + field);
        }
    }

    private void validateNormalizedBounds(JsonNode node, String label) throws IOException {
        for (String field : List.of("x", "y", "width", "height")) {
            double value = node.path(field).asDouble(-1);
            if (value < 0 || value > 1) {
                throw new IOException("OpenAI가 올바른 " + label + " 좌표를 반환하지 않았습니다: " + field);
            }
        }
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
        return CARD_CANVAS_INSTRUCTION + regionalInstruction(resource) + typeInstruction
                + ". Do not create a complete branded card, logo, or unrelated text. "
                + candidateInstruction(resource)
                + prompt + options;
    }

    private String candidateInstruction(AiResourceGeneration resource) {
        int candidateIndex = readCandidateIndex(resource.getGeneratedData());
        int candidateCount = readCandidateCount(resource.getGeneratedData());
        if (candidateCount <= 0) return "";
        return "This is recommendation candidate " + candidateIndex + " of " + candidateCount
                + ". Make it clearly different from the other candidates while preserving the same requirements. ";
    }

    private String regionalInstruction(AiResourceGeneration resource) {
        if (!usesRegionalVisual(resource.getResourceType()) || resource.getCard().getPurchaseStore() == null) {
            return "";
        }

        Store store = resource.getCard().getPurchaseStore();
        String city = store.getCity() == null ? "" : store.getCity().trim();
        String country = store.getCountry() == null ? "" : store.getCountry().trim();
        if (city.isBlank()) return "";

        List<String> landmarks = landmarksFor(city);
        int variant = readRegionalVariant(resource.getGeneratedData());
        String landmark = landmarks.get(Math.floorMod(variant, landmarks.size()));
        String location = country.isBlank() ? city : country + ", " + city;

        return "The purchase location is " + location + ". "
                + "Create a refined, original regional visual inspired by " + landmark + ". "
                + "Use the landmark as a tasteful silhouette, architectural detail, texture, or atmosphere; "
                + "do not copy a photograph, use logos, or add readable text. "
                + "This is regional variation " + (variant + 1) + ", so make it visibly different from other variations. ";
    }

    private boolean usesRegionalVisual(AiResourceType resourceType) {
        return resourceType == AiResourceType.BACKGROUND
                || resourceType == AiResourceType.PATTERN
                || resourceType == AiResourceType.DECORATION
                || resourceType == AiResourceType.COMPOSITION;
    }

    private int readRegionalVariant(String generatedData) {
        if (generatedData == null || generatedData.isBlank()) return 0;
        try {
            return Math.max(0, objectMapper.readTree(generatedData).path("_regionalVariant").asInt(0));
        } catch (IOException ignored) {
            return 0;
        }
    }

    private int readCandidateIndex(String generatedData) {
        if (generatedData == null || generatedData.isBlank()) return 1;
        try {
            return Math.max(1, objectMapper.readTree(generatedData).path("_candidateIndex").asInt(1));
        } catch (IOException ignored) {
            return 1;
        }
    }

    private int readCandidateCount(String generatedData) {
        if (generatedData == null || generatedData.isBlank()) return 0;
        try {
            return Math.max(0, objectMapper.readTree(generatedData).path("_candidateCount").asInt(0));
        } catch (IOException ignored) {
            return 0;
        }
    }

    private List<String> landmarksFor(String city) {
        String normalizedCity = city.toLowerCase(Locale.ROOT);
        if (normalizedCity.contains("서울") || normalizedCity.contains("seoul")) {
            return List.of("Gwanghwamun Gate", "N Seoul Tower", "the Han River night skyline", "Bukchon tiled rooftops");
        }
        if (normalizedCity.contains("부산") || normalizedCity.contains("busan")) {
            return List.of("Gwangan Bridge", "Haeundae coastline", "Gamcheon hillside houses", "Busan harbor lights");
        }
        if (normalizedCity.contains("제주") || normalizedCity.contains("jeju")) {
            return List.of("Hallasan mountain", "Jeju basalt stone walls", "Seongsan Ilchulbong", "the Jeju coast");
        }
        if (normalizedCity.contains("도쿄") || normalizedCity.contains("tokyo")) {
            return List.of("Tokyo Tower", "Asakusa temple rooftops", "Shibuya night lights", "the Tokyo skyline");
        }
        if (normalizedCity.contains("뉴욕") || normalizedCity.contains("new york")) {
            return List.of("the Manhattan skyline", "Brooklyn Bridge", "Central Park geometry", "Art Deco city details");
        }
        return List.of(city + " local architecture and landscape");
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
