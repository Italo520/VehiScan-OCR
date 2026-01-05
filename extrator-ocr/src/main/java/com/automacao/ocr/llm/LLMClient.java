package com.automacao.ocr.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LLMClient {

    private final HttpClient httpClient;
    private final String apiKey; // para Perplexity
    private final String baseUrl; // ex.: https://api.perplexity.ai ou http://localhost:8080/v1
    private final String model; // ex.: sonar-small-chat ou llama-local

    public LLMClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String chat(String systemPrompt, String userPromptJson) {
        try {
            String body = """
                    {
                      "model": "%s",
                      "temperature": 0.0,
                      "max_tokens": 512,
                      "messages": [
                        {"role": "system", "content": %s},
                        {"role": "user", "content": %s}
                      ]
                    }
                    """.formatted(
                    model,
                    quote(systemPrompt),
                    quote(userPromptJson));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json");

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new RuntimeException("Erro LLM: " + response.statusCode() + " - " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar LLM", e);
        }
    }

    private String quote(String s) {
        if (s == null)
            return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}
