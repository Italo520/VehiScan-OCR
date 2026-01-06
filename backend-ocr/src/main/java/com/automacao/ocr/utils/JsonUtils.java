package com.automacao.ocr.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String extrairContent(String rawResponse) {
        try {
            JsonNode root = mapper.readTree(rawResponse);
            // Navega em choices[0].message.content
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode message = root.get("choices").get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    // Remove blocos de código markdown se houver (```json ... ```)
                    return limparMarkdown(content);
                }
            }
            throw new RuntimeException("Formato de resposta inesperado do LLM: " + rawResponse);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar JSON do LLM", e);
        }
    }

    private static String limparMarkdown(String content) {
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            if (content.contains("```")) {
                content = content.substring(0, content.indexOf("```"));
            }
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            if (content.contains("```")) {
                content = content.substring(0, content.indexOf("```"));
            }
        }
        return content.trim();
    }
}
