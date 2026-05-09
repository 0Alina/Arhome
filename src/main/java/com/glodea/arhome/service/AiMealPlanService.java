package com.glodea.arhome.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glodea.arhome.dto.MealPlanGenerateRequest;

@Service
public class AiMealPlanService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;

    public AiMealPlanService(ObjectMapper objectMapper,
                             @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
                             @Value("${ollama.model:llama3.1:8b}") String model) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "http://localhost:11434";
        this.model = model != null && !model.isBlank() ? model.trim() : "llama3.1:8b";
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    }

    public JsonNode generatePlan(MealPlanGenerateRequest request) {
        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

            String systemPrompt = "You are a nutrition assistant. Generate a 7-day meal plan. "
                + "Return ONLY valid JSON. Do not include markdown or extra text.";

            String userPrompt = "Use this context to build a weekly plan. "
                + "Each day should have meals with simple recipes. "
                + "Keep recipes realistic and consistent with restrictions. "
                + "JSON schema:\n"
                + "{\n"
                + "  \"title\": string,\n"
                + "  \"summary\": string,\n"
                + "  \"days\": [\n"
                + "    {\n"
                + "      \"day\": string,\n"
                + "      \"meals\": [\n"
                + "        {\n"
                + "          \"name\": string,\n"
                + "          \"recipe\": {\n"
                + "            \"title\": string,\n"
                + "            \"ingredients\": [string],\n"
                + "            \"steps\": [string],\n"
                + "            \"time\": string,\n"
                + "            \"portion\": string,\n"
                + "            \"notes\": string\n"
                + "          }\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "Context:\n" + contextJson;

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("prompt", systemPrompt + "\n\n" + userPrompt);
            payload.put("stream", false);
            payload.put("format", "json");

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.get("response");
            if (contentNode == null || contentNode.isMissingNode()) {
                throw new IllegalStateException("AI response did not include content.");
            }

            String content = contentNode.asText();
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            throw new IllegalStateException("AI generation failed: " + ex.getMessage(), ex);
        }
    }
}
