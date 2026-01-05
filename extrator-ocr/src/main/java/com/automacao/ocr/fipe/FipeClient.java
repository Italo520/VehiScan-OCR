package com.automacao.ocr.fipe;

import com.automacao.ocr.fipe.dto.ModelosResponseDTO;
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

    private static final String BASE_URL = "https://parallelum.com.br/fipe/api/v1";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public FipeClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public List<ReferenciaFipeDTO> listarMarcas() {
        return getList("/carros/marcas");
    }

    public ModelosResponseDTO listarModelos(String codigoMarca) {
        String url = BASE_URL + "/carros/marcas/" + codigoMarca + "/modelos";
        return get(url, ModelosResponseDTO.class);
    }

    public List<ReferenciaFipeDTO> listarAnos(String codigoMarca, String codigoModelo) {
        return getList("/carros/marcas/" + codigoMarca + "/modelos/" + codigoModelo + "/anos");
    }

    public ValorFipeDTO consultarValor(String codigoMarca, String codigoModelo, String codigoAno) {
        String url = BASE_URL + "/carros/marcas/" + codigoMarca + "/modelos/" + codigoModelo + "/anos/" + codigoAno;
        return get(url, ValorFipeDTO.class);
    }

    private List<ReferenciaFipeDTO> getList(String endpoint) {
        try {
            String url = BASE_URL + endpoint;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Erro Fipe API: " + response.statusCode());
            }

            return mapper.readValue(response.body(), new TypeReference<List<ReferenciaFipeDTO>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar Fipe: " + endpoint, e);
        }
    }

    private <T> T get(String url, Class<T> clazz) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Erro Fipe API: " + response.statusCode());
            }

            return mapper.readValue(response.body(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar Fipe URL: " + url, e);
        }
    }
}
