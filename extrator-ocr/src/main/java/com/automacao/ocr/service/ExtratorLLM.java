package com.automacao.ocr.service;

import com.automacao.ocr.dto.DocumentoVeiculoDTO;

public interface ExtratorLLM {
    DocumentoVeiculoDTO extrairCampos(String textoDocumento, DocumentoVeiculoDTO dadosPreliminares);
}
