package com.automacao.ocr.service.validator;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public class ConsultaValidator extends AbstractDocumentValidator {

    @Override
    protected void validarEspecifico(DocumentoVeiculoDTO doc, String textoOriginal) {
        // Validações específicas para Consulta
        // Consultas podem não ter todos os dados, mas Placa e Chassi são essenciais
        validarPlaca(doc);
        validarChassi(doc);
    }
}
