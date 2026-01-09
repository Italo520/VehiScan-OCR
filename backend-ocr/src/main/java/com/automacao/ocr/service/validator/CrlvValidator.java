package com.automacao.ocr.service.validator;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public class CrlvValidator extends AbstractDocumentValidator {

    @Override
    protected void validarEspecifico(DocumentoVeiculoDTO doc, String textoOriginal) {
        // Validações específicas para CRLV
        // Ex: CRLV sempre deve ter Renavam
        validarRenavam(doc);

        // Ex: CRLV geralmente tem CPF/CNPJ do proprietário
        validarCpfCnpj(doc);
    }
}
