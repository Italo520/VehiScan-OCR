package com.automacao.ocr.service;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public interface ValidadorDocumentoVeiculo {
    DocumentoVeiculoDTO validar(DocumentoVeiculoDTO doc, String textoOriginal);
}
