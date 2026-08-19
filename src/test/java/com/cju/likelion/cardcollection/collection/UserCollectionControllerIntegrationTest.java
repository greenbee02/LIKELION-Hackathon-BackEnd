package com.cju.likelion.cardcollection.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserCollectionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void ownerCanManageCustomCollectionAndCannotAddTheSameCardTwice() throws Exception {
        String ownerEmail = "collection-owner-" + UUID.randomUUID() + "@example.com";
        signup(ownerEmail);
        String ownerToken = login(ownerEmail);

        String createResponse = mockMvc.perform(post("/api/v1/collections")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"서울 컬렉션\",\"description\":\"서울에서 구매한 카드\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.collectionType").value("CUSTOM"))
                .andExpect(jsonPath("$.data.cardCount").value(0))
                .andReturn().getResponse().getContentAsString();
        String collectionId = json(createResponse).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/collections").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(collectionId));

        String cardResponse = mockMvc.perform(post("/api/v1/cards/registrations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"MCM-DEMO-2026-001\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cardId = json(cardResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/collections/" + collectionId + "/cards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardId\":\"" + cardId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cardCount").value(1))
                .andExpect(jsonPath("$.data.cards[0].id").value(cardId));

        mockMvc.perform(post("/api/v1/collections/" + collectionId + "/cards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardId\":\"" + cardId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COLLECTION_CARD_ALREADY_ADDED"));

        String otherEmail = "collection-other-" + UUID.randomUUID() + "@example.com";
        signup(otherEmail);
        String otherToken = login(otherEmail);
        mockMvc.perform(get("/api/v1/collections/" + collectionId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COLLECTION_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/collections/" + collectionId + "/cards/" + cardId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    private void signup(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"컬렉션 사용자\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json(response).path("data").path("accessToken").asText();
    }

    private JsonNode json(String value) throws Exception { return objectMapper.readTree(value); }
}
