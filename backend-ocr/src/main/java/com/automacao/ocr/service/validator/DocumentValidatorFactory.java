package com.automacao.ocr.service.validator;

import com.automacao.ocr.model.DocumentoVeiculoDTO;

public class DocumentValidatorFactory {

    public static DocumentValidatorStrategy getValidator(DocumentoVeiculoDTO doc) {
        if (doc == null || doc.getTipoDocumento() == null || doc.getTipoDocumento().getValor() == null) {
            return new GenericValidator();
        }

        String tipo = doc.getTipoDocumento().getValor().toUpperCase();

        if (tipo.contains("CRV") || tipo.contains("CRLV")) {
            return new CrlvValidator();
        } else if (tipo.contains("CONSULTA")) {
            return new ConsultaValidator();
        } else {
            return new GenericValidator();
        }
    }
}
