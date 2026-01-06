package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenciaFipeDTO {
    @JsonProperty("code")
    public String codigo;

    @JsonProperty("name")
    public String nome;
}
