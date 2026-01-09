package com.automacao.ocr.service;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.service.validator.DocumentValidatorFactory;
import com.automacao.ocr.service.validator.DocumentValidatorStrategy;

public class ValidadorDocumentoVeiculoImpl implements ValidadorDocumentoVeiculo {

    @Override
    public DocumentoVeiculoDTO validar(DocumentoVeiculoDTO doc, String textoOriginal) {
        DocumentValidatorStrategy validator = DocumentValidatorFactory.getValidator(doc);
        validator.validar(doc, textoOriginal);
        return doc;
    }
}
