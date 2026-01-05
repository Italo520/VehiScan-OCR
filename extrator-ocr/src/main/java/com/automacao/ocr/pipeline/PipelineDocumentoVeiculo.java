package com.automacao.ocr.pipeline;

import com.automacao.ocr.ExtratorTexto;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;
import com.automacao.ocr.service.ExtratorLLM;
import com.automacao.ocr.service.ValidadorDocumentoVeiculo;

import java.io.File;
import java.io.IOException;

public class PipelineDocumentoVeiculo {

    private final ExtratorTexto extratorTexto;
    private final ExtratorLLM extratorLLM;
    private final ValidadorDocumentoVeiculo validador;

    public PipelineDocumentoVeiculo(ExtratorTexto extratorTexto,
            ExtratorLLM extratorLLM,
            ValidadorDocumentoVeiculo validador) {
        this.extratorTexto = extratorTexto;
        this.extratorLLM = extratorLLM;
        this.validador = validador;
    }

    public DocumentoVeiculoDTO processar(File arquivo) throws IOException {
        // 1. OCR Puro
        System.out.println("   [Pipeline] 1. Executando OCR...");
        var resultadoTexto = extratorTexto.extrairTexto(arquivo);
        String texto = resultadoTexto.getTextoCompleto();

        if (texto == null || texto.isBlank()) {
            System.out.println("   [Pipeline] AVISO: OCR não retornou texto.");
            return new DocumentoVeiculoDTO(); // Retorna vazio
        }

        // 2. Extração Preliminar (Regex)
        System.out.println("   [Pipeline] 2a. Extração Preliminar (Regex)...");
        // Precisamos instanciar o extrator de regex aqui ou injetá-lo.
        // Como ele não implementa ExtratorLLM, usamos direto.
        com.automacao.ocr.ExtratorDadosVeiculo extratorRegex = new com.automacao.ocr.ExtratorDadosVeiculo();
        var dadosMap = extratorRegex.extrairDados(texto);

        // Converte Map -> DTO para passar como sugestão
        DocumentoVeiculoDTO dadosPreliminares = converterMapParaDTO(dadosMap);

        // 3. Extração Final (LLM com revisão)
        System.out.println("   [Pipeline] 2b. Extração Final (LLM revisando Regex)...");
        DocumentoVeiculoDTO extraido = extratorLLM.extrairCampos(texto, dadosPreliminares);

        // 4. Validação e Regras de Negócio
        System.out.println("   [Pipeline] 3. Validando dados...");
        DocumentoVeiculoDTO validado = validador.validar(extraido, texto);

        return validado;
    }

    private DocumentoVeiculoDTO converterMapParaDTO(java.util.Map<String, String> map) {
        DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();
        dto.setPlaca(new com.automacao.ocr.dto.CampoExtraido(map.get("Placa"), com.automacao.ocr.dto.CampoStatus.OK,
                "Regex"));
        dto.setChassi(new com.automacao.ocr.dto.CampoExtraido(map.get("Chassi"), com.automacao.ocr.dto.CampoStatus.OK,
                "Regex"));
        dto.setFabricacao(new com.automacao.ocr.dto.CampoExtraido(map.get("Fabricação"),
                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
        dto.setModelo(new com.automacao.ocr.dto.CampoExtraido(map.get("Ano Modelo"),
                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
        dto.setMarca(new com.automacao.ocr.dto.CampoExtraido(map.get("Marca/Modelo"),
                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
        dto.setTipoDocumento(new com.automacao.ocr.dto.CampoExtraido(map.get("Tipo Documento"),
                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
        return dto;
    }
}
