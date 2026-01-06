package com.automacao.ocr.service;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public interface ExtratorLLM {
    DocumentoVeiculoDTO extrairCampos(String textoDocumento, DocumentoVeiculoDTO dadosPreliminares);
}
