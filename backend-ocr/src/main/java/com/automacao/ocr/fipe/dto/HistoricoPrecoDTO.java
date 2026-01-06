package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricoPrecoDTO {
    public String month;
    public String price;
    public String reference;
}
