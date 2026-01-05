package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValorFipeDTO {
    @JsonProperty("Valor")
    public String valor;

    @JsonProperty("Marca")
    public String marca;

    @JsonProperty("Modelo")
    public String modelo;

    @JsonProperty("AnoModelo")
    public Integer anoModelo;

    @JsonProperty("Combustivel")
    public String combustivel;

    @JsonProperty("CodigoFipe")
    public String codigoFipe;

    @JsonProperty("MesReferencia")
    public String mesReferencia;

    @JsonProperty("TipoVeiculo")
    public Integer tipoVeiculo;

    @JsonProperty("SiglaCombustivel")
    public String siglaCombustivel;
}
