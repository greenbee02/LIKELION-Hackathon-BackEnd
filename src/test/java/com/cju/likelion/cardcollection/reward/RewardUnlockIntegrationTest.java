package com.cju.likelion.cardcollection.reward;

import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.reward.repository.UserRewardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RewardUnlockIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired UserRewardRepository userRewards;

    @Test
    void seoulExclusiveUnlocksEventAtTwoProductsAndRewardAtThreeProducts() throws Exception {
        String email = "reward-" + UUID.randomUUID() + "@example.com";
        signup(email);
        String token = login(email);

        issue(token, "MCM-DEMO-2026-003");
        issue(token, "MCM-DEMO-2026-004");
        UUID userId = users.findByEmail(email).orElseThrow().getId();

        assertThat(userRewards.existsByUserIdAndEventId(userId, UUID.fromString("80000000-0000-0000-0000-000000000001"))).isTrue();
        assertThat(userRewards.existsByUserIdAndRewardId(userId, UUID.fromString("70000000-0000-0000-0000-000000000001"))).isFalse();

        mockMvc.perform(get("/api/v1/rewards/progress/40000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.requiredProductCount").value(3))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.ownedRequiredProductCount").value(2))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.requiredProducts[?(@.owned == true)]").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.requiredProducts[?(@.owned == true)].cards[0].frontImageUrl").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.targets[?(@.type == 'EVENT')].event.location").exists());

        issue(token, "MCM-DEMO-2026-008");
        assertThat(userRewards.existsByUserIdAndRewardId(userId, UUID.fromString("70000000-0000-0000-0000-000000000001"))).isTrue();
        assertThat(userRewards.findByUserId(userId)).hasSize(2);

        mockMvc.perform(get("/api/v1/rewards/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].percentage").exists());

        String myResponse = mockMvc.perform(get("/api/v1/rewards/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String rewardId = "";
        for (JsonNode item : json(myResponse).path("data")) {
            if ("REWARD".equals(item.path("targetType").asText())) {
                rewardId = item.path("id").asText();
            }
        }
        assertThat(rewardId).isNotBlank();
        mockMvc.perform(post("/api/v1/rewards/" + rewardId + "/claim").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.claimCode").isNotEmpty());
    }

    private void issue(String token, String qrToken) throws Exception { mockMvc.perform(post("/api/v1/cards/registrations").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"qrToken\":\"" + qrToken + "\"}")).andExpect(status().isCreated()); }
    private void signup(String email) throws Exception { mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"password123\",\"name\":\"리워드 사용자\"}")).andExpect(status().isCreated()); }
    private String login(String email) throws Exception { String response=mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); return json(response).path("data").path("accessToken").asText(); }
    private JsonNode json(String value) throws Exception { return objectMapper.readTree(value); }
}
