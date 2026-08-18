package com.cju.likelion.cardcollection.card;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.auth.domain.UserRole;
import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.card.domain.CardType;
import com.cju.likelion.cardcollection.card.domain.PurchaseQr;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import com.cju.likelion.cardcollection.card.repository.CardRepository;
import com.cju.likelion.cardcollection.card.repository.PurchaseQrRepository;
import com.cju.likelion.cardcollection.card.service.CardService;
import com.cju.likelion.cardcollection.ai.provider.AiImageProvider;
import com.cju.likelion.cardcollection.ai.provider.AiImageResult;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.ai.storage.GeneratedImageStorage;
import com.cju.likelion.cardcollection.ai.worker.AiResourceGenerationWorker;
import com.cju.likelion.cardcollection.catalog.domain.Brand;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.Store;
import com.cju.likelion.cardcollection.catalog.repository.BrandRepository;
import com.cju.likelion.cardcollection.catalog.repository.CardTemplateRepository;
import com.cju.likelion.cardcollection.catalog.repository.ProductRepository;
import com.cju.likelion.cardcollection.catalog.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CardTemplateRepository templateRepository;

    @Autowired
    private PurchaseQrRepository purchaseQrRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private AiResourceGenerationRepository aiResourceRepository;

    @Test
    void registerListAndGetCard() throws Exception {
        Fixture fixture = fixture(false);
        String email = uniqueEmail();
        signup(email);
        String token = login(email);

        String response = mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"%s\"}".formatted(fixture.qrToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cardType").value("BASIC"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.product.name").value(fixture.product().getName()))
                .andExpect(jsonPath("$.data.store.name").value(fixture.store().getName()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cardId = objectMapper.readTree(response).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/cards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(cardId));

        mockMvc.perform(get("/api/v1/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(cardId));
    }

    @Test
    void duplicateQrReturnsConflict() throws Exception {
        Fixture fixture = fixture(false);
        String email = uniqueEmail();
        signup(email);
        String token = login(email);
        String body = "{\"qrToken\":\"%s\"}".formatted(fixture.qrToken());

        mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QR_ALREADY_USED"));
    }

    @Test
    void customizationSelectionAndOriginalRestore() throws Exception {
        Fixture fixture = fixture(false);
        String email = uniqueEmail();
        signup(email);
        String token = login(email);
        String cardResponse = mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"%s\"}".formatted(fixture.qrToken())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cardId = objectMapper.readTree(cardResponse).path("data").path("id").asText();

        String customizationResponse = mockMvc.perform(post("/api/v1/cards/" + cardId + "/customizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputText\":\"나의 첫 컬렉션\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        String customizationId = objectMapper.readTree(customizationResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/customizations/" + customizationId + "/select")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardType").value("CUSTOMIZE"))
                .andExpect(jsonPath("$.data.selectedCustomization.id").value(customizationId));

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/restore-original")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardType").value("BASIC"))
                .andExpect(jsonPath("$.data.selectedCustomization").doesNotExist());
    }

    @Test
    void aiResourceGenerationRequestIsStoredAsPendingAndCanBeQueried() throws Exception {
        Fixture fixture = fixture(false);
        String email = uniqueEmail();
        signup(email);
        String token = login(email);
        String cardResponse = mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"%s\"}".formatted(fixture.qrToken())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cardId = objectMapper.readTree(cardResponse).path("data").path("id").asText();

        String resourceResponse = mockMvc.perform(post("/api/v1/cards/" + cardId + "/ai-resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceType": "PRODUCT_ANGLE",
                                  "prompt": "상품을 오른쪽 45도에서 본 이미지",
                                  "sourceImageUrl": "https://example.com/product.png",
                                  "options": {"angle": 45, "background": "transparent"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.resourceType").value("PRODUCT_ANGLE"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.productId").value(fixture.product().getId().toString()))
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(resourceResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/ai-resources/compose")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceIds\":[\"%s\"]}".formatted(resourceId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_RESOURCE_NOT_COMPLETED"));

        mockMvc.perform(get("/api/v1/cards/" + cardId + "/ai-resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(resourceId));

        mockMvc.perform(get("/api/v1/cards/" + cardId + "/ai-resources/" + resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.generatedImageUrl").doesNotExist());
    }

    @Test
    void aiWorkerCompletesPendingResourceAndStoresGeneratedImageUrl() throws Exception {
        Fixture fixture = fixture(false);
        String email = uniqueEmail();
        signup(email);
        String token = login(email);
        String cardResponse = mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"%s\"}".formatted(fixture.qrToken())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cardId = objectMapper.readTree(cardResponse).path("data").path("id").asText();

        String resourceResponse = mockMvc.perform(post("/api/v1/cards/" + cardId + "/ai-resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"BACKGROUND\",\"prompt\":\"dark velvet\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID resourceId = UUID.fromString(objectMapper.readTree(resourceResponse).path("data").path("id").asText());

        AiImageProvider provider = mock(AiImageProvider.class);
        GeneratedImageStorage storage = mock(GeneratedImageStorage.class);
        when(provider.generate(any())).thenReturn(new AiImageResult(
                new byte[]{1, 2, 3}, "image/png", "test-image-model"));
        when(storage.store(any(), any(), any())).thenReturn("/generated/ai-resources/" + resourceId + ".png");

        new AiResourceGenerationWorker(
                aiResourceRepository,
                provider,
                storage,
                true,
                "test-api-key"
        ).process(resourceId);

        mockMvc.perform(get("/api/v1/cards/" + cardId + "/ai-resources/" + resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.generatedImageUrl")
                        .value("/generated/ai-resources/" + resourceId + ".png"));

        String compositionResponse = mockMvc.perform(post("/api/v1/cards/" + cardId + "/ai-resources/compose")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceIds": ["%s"],
                                  "message": "나만의 카드",
                                  "layoutData": {"productX": 0.5, "productY": 0.55}
                                }
                                """.formatted(resourceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.card.id").value(cardId))
                .andExpect(jsonPath("$.data.card.cardType").value("CUSTOMIZE"))
                .andExpect(jsonPath("$.data.card.selectedCustomization.id").exists())
                .andExpect(jsonPath("$.data.customization.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.customization.aiModel").value("composition-v1"))
                .andReturn().getResponse().getContentAsString();
        assertThat(compositionResponse).contains(resourceId.toString());
    }

    @Test
    void concurrentRegistrationAllowsOnlyOneRequest() throws Exception {
        Fixture fixture = fixture(false);
        User user = userRepository.save(User.builder()
                .email(uniqueEmail())
                .name("동시성 테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<String>> results = List.of(
                    executor.submit(() -> registerFromService(start, user.getId(), fixture.qrToken())),
                    executor.submit(() -> registerFromService(start, user.getId(), fixture.qrToken()))
            );
            start.countDown();

            List<String> codes = results.stream().map(this::getResult).toList();
            assertEquals(1, codes.stream().filter("SUCCESS"::equals).count());
            assertEquals(1, codes.stream().filter("QR_ALREADY_USED"::equals).count());
            assertEquals(1, cardRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
        } finally {
            executor.shutdownNow();
        }
    }

    private String registerFromService(CountDownLatch start, UUID userId, String qrToken) throws Exception {
        start.await();
        try {
            cardService.register(userId, new com.cju.likelion.cardcollection.card.dto.CardRegistrationRequest(qrToken));
            return "SUCCESS";
        } catch (CardDomainException exception) {
            return exception.getCode();
        }
    }

    private String getResult(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Fixture fixture(boolean limited) {
        Brand brand = brandRepository.save(Brand.of("테스트 브랜드 " + UUID.randomUUID()));
        Store store = storeRepository.save(Store.of(brand, "테스트 매장", "KR", "Seoul"));
        Product product = productRepository.save(Product.of(brand, "테스트 상품", limited));
        templateRepository.save(CardTemplate.of(
                brand,
                "테스트 템플릿 " + UUID.randomUUID(),
                "/images/templates/template_001_front.png",
                "/images/templates/template_001_back.png",
                null
        ));
        String qrToken = "TEST-QR-" + UUID.randomUUID();
        purchaseQrRepository.save(PurchaseQr.of(
                qrToken,
                product,
                store,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        ));
        return new Fixture(qrToken, product, store);
    }

    private void signup(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "name": "카드 테스트 사용자"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("accessToken").asText();
    }

    private String uniqueEmail() {
        return "card-test-" + UUID.randomUUID() + "@example.com";
    }

    private record Fixture(String qrToken, Product product, Store store) {
    }
}
