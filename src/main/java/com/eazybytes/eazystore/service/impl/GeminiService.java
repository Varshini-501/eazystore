package com.eazybytes.eazystore.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Google's Gemini API (free tier, no billing required)
 * used to generate a short promotional blurb for a vendor's product.
 * Set the GEMINI_API_KEY environment variable to enable this.
 */
@Service
public class GeminiService {

    private final RestClient restClient;

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    @Value("${GEMINI_MODEL:gemini-2.0-flash-lite}")
    private String model;

    public GeminiService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String generatePromoText(String productName, String productDescription, BigDecimal price) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI promo generation isn't configured yet. Set the GEMINI_API_KEY environment variable.");
        }

        String prompt = """
                Write one short, exciting promotional social media post (strictly under 220 characters) \
                for an online store's product feed. Use exactly one or two relevant emojis. \
                Do not use hashtags. Do not wrap the output in quotation marks. Output only the post text, nothing else.

                Product name: %s
                Description: %s
                Price: Rs. %s
                """.formatted(productName, productDescription, price);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.9, "maxOutputTokens", 200)
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
            return extractText(response);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Couldn't reach the AI service right now. Please try again in a moment.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");
            return text == null ? "" : text.trim();
        } catch (Exception ex) {
            throw new IllegalStateException("The AI service returned an unexpected response. Please try again.");
        }
    }
}
