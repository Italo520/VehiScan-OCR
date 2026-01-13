package com.automacao.auditoria.strategy;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.service.CsvExportService;
import java.io.File;

public class CsvExportStrategy implements ExportStrategy {

    private static final String DEFAULT_FILENAME = "veiculos_exportados.csv";

    @Override
    public void exportar(DocumentoVeiculoDTO documento, File pastaDestino) throws Exception {
        if (pastaDestino == null || !pastaDestino.isDirectory()) {
            throw new IllegalArgumentException("Pasta de destino inválida.");
        }

        // Define o arquivo de destino (sempre o mesmo para acumular dados)
        File arquivoDestino = new File(pastaDestino, DEFAULT_FILENAME);

        // Usa o serviço centralizado para garantir formatação consistente
        CsvExportService csvService = new CsvExportService(arquivoDestino.getAbsolutePath());
        csvService.salvarVeiculo(documento);
    }
}
