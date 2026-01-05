package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelosResponseDTO {
    public java.util.List<ReferenciaFipeDTO> modelos;
    public java.util.List<ReferenciaFipeDTO> anos;
}
