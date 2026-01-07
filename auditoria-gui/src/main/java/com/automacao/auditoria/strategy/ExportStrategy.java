package com.automacao.auditoria.strategy;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import java.io.File;

public interface ExportStrategy {
    void exportar(DocumentoVeiculoDTO documento, File destino) throws Exception;
}
