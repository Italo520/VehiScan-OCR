package com.automacao.auditoria.strategy;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CsvExportStrategy implements ExportStrategy {

    @Override
    public void exportar(DocumentoVeiculoDTO documento, File pastaDestino) throws Exception {
        if (pastaDestino == null || !pastaDestino.isDirectory()) {
            throw new IllegalArgumentException("Pasta de destino inválida.");
        }

        String placa = documento.getPlaca() != null ? documento.getPlaca().getValor() : "SEM_PLACA";
        String chassi = documento.getChassi() != null ? documento.getChassi().getValor() : "SEM_CHASSI";
        String status = documento.getStatusExtracao() != null ? documento.getStatusExtracao().toString()
                : "DESCONHECIDO";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s_%s.csv", placa, chassi, status, timestamp);

        File arquivo = new File(pastaDestino, fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(arquivo))) {
            // Header
            writer.println("Placa;Chassi;Marca;Modelo;Ano;Renavam;CPF_CNPJ;Status;Data_Auditoria");

            // Data
            writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                    escape(placa),
                    escape(chassi),
                    escape(documento.getMarca() != null ? documento.getMarca().getValor() : ""),
                    escape(documento.getModelo() != null ? documento.getModelo().getValor() : ""),
                    escape(documento.getFabricacao() != null ? documento.getFabricacao().getValor() : ""),
                    escape(documento.getRenavam() != null ? documento.getRenavam().getValor() : ""),
                    escape(documento.getCpfCnpj() != null ? documento.getCpfCnpj().getValor() : ""),
                    status,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    private String escape(String data) {
        if (data == null)
            return "";
        return data.replace(";", ","); // Simple escape for CSV
    }
}
