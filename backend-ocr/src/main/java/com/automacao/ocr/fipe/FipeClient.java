package com.automacao.ocr.fipe;

import com.automacao.ocr.fipe.dto.ReferenciaFipeDTO;
import com.automacao.ocr.fipe.dto.ValorFipeDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class FipeClient {

    private static final String BASE_URL = "https://fipe.parallelum.com.br/api/v2";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String subscriptionToken;

    public FipeClient() {
        // Carrega token do .env
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .load();

        this.subscriptionToken = dotenv.get("FIPE_SUBSCRIPTION_TOKEN");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();

        if (subscriptionToken == null || subscriptionToken.isEmpty()) {
            System.err.println("[AVISO] FIPE_SUBSCRIPTION_TOKEN não configurado no .env - As consultas podem falhar!");
        }
    }

    public List<ReferenciaFipeDTO> listarMarcas() {
        return getList("/cars/brands");
    }

    public List<ReferenciaFipeDTO> listarModelos(String codigoMarca) {
        return getList("/cars/brands/" + codigoMarca + "/models");
    }

    public List<ReferenciaFipeDTO> listarAnos(String codigoMarca, String codigoModelo) {
        return getList("/cars/brands/" + codigoMarca + "/models/" + codigoModelo + "/years");
    }

    public ValorFipeDTO consultarValor(String codigoMarca, String codigoModelo, String codigoAno) {
        String url = BASE_URL + "/cars/brands/" + codigoMarca + "/models/" + codigoModelo + "/years/" + codigoAno;
        return get(url, ValorFipeDTO.class);
    }

    private List<ReferenciaFipeDTO> getList(String endpoint) {
        try {
            String url = BASE_URL + endpoint;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .GET();

            // Adiciona token se disponível
            if (subscriptionToken != null && !subscriptionToken.isEmpty()) {
                requestBuilder.header("X-Subscription-Token", subscriptionToken);
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[Fipe API] Erro " + response.statusCode() + ": " + response.body());
                throw new RuntimeException("Erro Fipe API v2: " + response.statusCode());
            }

            return mapper.readValue(response.body(), new TypeReference<List<ReferenciaFipeDTO>>() {
            });

        } catch (Exception e) {
            System.err.println("[Fipe API] Falha ao consultar: " + endpoint + " - " + e.getMessage());
            throw new RuntimeException("Falha ao consultar Fipe: " + endpoint, e);
        }
    }

    private <T> T get(String url, Class<T> clazz) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .GET();

            // Adiciona token se disponível
            if (subscriptionToken != null && !subscriptionToken.isEmpty()) {
                requestBuilder.header("X-Subscription-Token", subscriptionToken);
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[Fipe API] Erro " + response.statusCode() + ": " + response.body());
                throw new RuntimeException("Erro Fipe API v2: " + response.statusCode());
            }

            return mapper.readValue(response.body(), clazz);

        } catch (Exception e) {
            System.err.println("[Fipe API] Falha ao consultar: " + url + " - " + e.getMessage());
            throw new RuntimeException("Falha ao consultar Fipe URL: " + url, e);
        }
    }
}
