package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FipeCompletoDTO {
    @JsonProperty("brand")
    public String brand;

    @JsonProperty("codeFipe")
    public String codeFipe;

    @JsonProperty("fuel")
    public String fuel;

    @JsonProperty("fuelAcronym")
    public String fuelAcronym;

    @JsonProperty("model")
    public String model;

    @JsonProperty("modelYear")
    public Integer modelYear;

    @JsonProperty("price")
    public String price;

    @JsonProperty("referenceMonth")
    public String referenceMonth;

    @JsonProperty("vehicleType")
    public Integer vehicleType;

    @JsonProperty("priceHistory")
    public List<HistoricoPrecoDTO> priceHistory;

    // Construtor vazio para Jackson
    public FipeCompletoDTO() {
    }

    // Construtor de conveniência para mapear do ValorFipeDTO
    public FipeCompletoDTO(ValorFipeDTO v1) {
        this.brand = v1.marca;
        this.codeFipe = v1.codigoFipe;
        this.fuel = v1.combustivel;
        this.fuelAcronym = v1.siglaCombustivel;
        this.model = v1.modelo;
        this.modelYear = v1.anoModelo;
        this.price = v1.valor;
        this.referenceMonth = v1.mesReferencia;
        this.vehicleType = v1.tipoVeiculo;
        // priceHistory não vem na consulta padrão
    }
}
