package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValorFipeDTO {
    // API v2 usa nomes diferentes
    @JsonProperty("price")
    public String valor;

    @JsonProperty("brand")
    public String marca;

    @JsonProperty("model")
    public String modelo;

    @JsonProperty("modelYear")
    public Integer anoModelo;

    @JsonProperty("fuel")
    public String combustivel;

    @JsonProperty("codeFipe")
    public String codigoFipe;

    @JsonProperty("referenceMonth")
    public String mesReferencia;

    @JsonProperty("vehicleType")
    public Integer tipoVeiculo;

    @JsonProperty("fuelAcronym")
    public String siglaCombustivel;
}
