package com.automacao.ocr.service;

import com.automacao.ocr.ExtratorDadosVeiculo;
import com.automacao.ocr.dto.CampoExtraido;
import com.automacao.ocr.dto.CampoStatus;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;

import java.util.Map;

/**
 * Implementação simulada que usa Regex local em vez de chamar um LLM real.
 * Em produção, esta classe faria uma chamada HTTP para
 * OpenAI/Perplexity/LocalLLM.
 */
public class ExtratorLLMSimulado implements ExtratorLLM {

    private final ExtratorDadosVeiculo extratorRegex;

    public ExtratorLLMSimulado() {
        this.extratorRegex = new ExtratorDadosVeiculo();
    }

    @Override
    public DocumentoVeiculoDTO extrairCampos(String textoDocumento, DocumentoVeiculoDTO dadosPreliminares) {
        // No simulado, ignoramos os dados preliminares e rodamos o regex novamente
        // (ou poderíamos retornar os preliminares se já viessem preenchidos)
        if (dadosPreliminares != null) {
            return dadosPreliminares;
        }
        Map<String, String> dadosBrutos = extratorRegex.extrairDados(textoDocumento);

        DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();

        dto.setPlaca(criarCampo(dadosBrutos.get("Placa")));
        dto.setChassi(criarCampo(dadosBrutos.get("Chassi")));
        dto.setFabricacao(criarCampo(dadosBrutos.get("Fabricação")));
        dto.setModelo(criarCampo(dadosBrutos.get("Ano Modelo")));
        dto.setMarca(criarCampo(dadosBrutos.get("Marca/Modelo")));
        dto.setTipoDocumento(criarCampo(dadosBrutos.get("Tipo Documento")));

        // Campos não capturados pelo regex atual, mas que o LLM poderia pegar
        dto.setClassificacao(criarCampo(null));
        dto.setObservacoes(criarCampo(null));

        return dto;
    }

    private CampoExtraido criarCampo(String valor) {
        if (valor == null) {
            return new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Não extraído pelo Regex");
        }
        // O LLM "cru" retornaria o valor sem validar status ainda
        return new CampoExtraido(valor, CampoStatus.OK, "Extraído via Simulação Regex");
    }
}
