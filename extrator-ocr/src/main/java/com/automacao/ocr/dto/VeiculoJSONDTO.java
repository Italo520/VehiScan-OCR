package com.automacao.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VeiculoJSONDTO {
    public String tipo_documento;
    public String placa;
    public String marca;
    public String fabricacao;
    public String modelo;
    public String chassi;
    public String classificacao;
    public String observacoes;
}
