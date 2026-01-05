package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenciaFipeDTO {
    public String codigo;
    public String nome;
}
