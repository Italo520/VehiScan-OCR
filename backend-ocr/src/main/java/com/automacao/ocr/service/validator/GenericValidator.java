package com.automacao.ocr.service.validator;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public class GenericValidator extends AbstractDocumentValidator {

    @Override
    protected void validarEspecifico(DocumentoVeiculoDTO doc, String textoOriginal) {
        // Validação genérica, tenta validar tudo que estiver disponível
        // Não impõe regras estritas de presença de campos além do básico
    }
}
