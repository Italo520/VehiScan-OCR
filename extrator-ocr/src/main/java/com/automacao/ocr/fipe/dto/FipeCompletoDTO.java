package com.automacao.ocr.fipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FipeCompletoDTO {
    public String brand;
    public String codeFipe;
    public String fuel;
    public String fuelAcronym;
    public String model;
    public Integer modelYear;
    public String price;
    public String referenceMonth;
    public Integer vehicleType;
    public List<HistoricoPrecoDTO> priceHistory;

    // Construtor vazio para Jackson
    public FipeCompletoDTO() {
    }

    // Construtor de conveniência para mapear do ValorFipeDTO (v1)
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
        // priceHistory não vem na v1 padrão, ficaria null ou vazio
    }
}
