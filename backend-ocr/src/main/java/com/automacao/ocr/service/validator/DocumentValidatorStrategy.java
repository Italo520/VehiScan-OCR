package com.automacao.ocr.service.validator;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public interface DocumentValidatorStrategy {
    /**
     * Valida o documento e atualiza os status dos campos.
     * 
     * @param doc           O DTO do documento a ser validado.
     * @param textoOriginal O texto bruto extraído (opcional, para validações
     *                      contextuais).
     */
    void validar(DocumentoVeiculoDTO doc, String textoOriginal);
}
