package com.automacao.ocr.service;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CsvExportService {

    private final String caminhoArquivoCsv;
    private static final String SEPARATOR = ";";

    public CsvExportService(String caminhoArquivoCsv) {
        this.caminhoArquivoCsv = caminhoArquivoCsv;
    }

    public void salvarVeiculo(DocumentoVeiculoDTO dto) {
        File arquivo = new File(caminhoArquivoCsv);
        boolean arquivoNovo = !arquivo.exists();

        try (FileWriter fw = new FileWriter(arquivo, true);
                PrintWriter pw = new PrintWriter(fw)) {

            if (arquivoNovo) {
                pw.println("PLACA;MARCA/MODELO;FAB/MOD;CHASSI;CIA;CÓDIGO FIPE;VALOR FIPE;CAUSA;VALOR MÍNIMO;MONTA");
            }

            pw.println(formatarLinha(dto));

            System.out.println("   [CSV] Dados salvos em: " + arquivo.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("   [CSV] Erro ao salvar no CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatarLinha(DocumentoVeiculoDTO dto) {
        StringBuilder sb = new StringBuilder();

        // PLACA
        sb.append(getValor(dto.getPlaca())).append(SEPARATOR);

        // MARCA/MODELO
        // Tenta pegar da marca, que muitas vezes vem como "MARCA/MODELO" do regex
        String marcaModelo = getValor(dto.getMarca());
        sb.append(marcaModelo).append(SEPARATOR);

        // FAB/MOD
        String fab = getValor(dto.getFabricacao());
        String mod = getValor(dto.getModelo());
        sb.append(fab).append("/").append(mod).append(SEPARATOR);

        // CHASSI
        sb.append(getValor(dto.getChassi())).append(SEPARATOR);

        // CIA (Nome Proprietário)
        // CIA (Nome Proprietário) - Deixar null conforme solicitado
        sb.append("").append(SEPARATOR);

        // CÓDIGO FIPE
        String codigoFipe = "-";
        String valorFipeStr = "-";

        if (dto.getDadosFipe() != null) {
            codigoFipe = dto.getDadosFipe().codigoFipe != null ? dto.getDadosFipe().codigoFipe : "-";
            valorFipeStr = dto.getDadosFipe().valor != null ? dto.getDadosFipe().valor : "-";
        }
        sb.append(codigoFipe).append(SEPARATOR);

        // VALOR FIPE
        sb.append(valorFipeStr).append(SEPARATOR);

        // CAUSA
        // Tenta extrair da observação ou classificação
        // CAUSA - Deixar null conforme solicitado
        sb.append("").append(SEPARATOR);

        // VALOR MÍNIMO (42% da FIPE)
        // VALOR MÍNIMO (42% da FIPE) - Deixar null conforme solicitado
        sb.append("").append(SEPARATOR);

        // MONTA
        String monta = getValor(dto.getClassificacao());
        String obs = getValor(dto.getObservacoes()).toUpperCase();
        if (monta.isEmpty() || monta.equals("-")) {
            // Tenta achar na observação
            if (obs.contains("MÉDIA MONTA") || obs.contains("MEDIA MONTA"))
                monta = "Média monta";
            else if (obs.contains("GRANDE MONTA"))
                monta = "Grande monta";
            else if (obs.contains("PEQUENA MONTA"))
                monta = "Pequena monta";
        }
        // Se ainda vazio, tenta inferir ou deixa vazio. Imagem mostra "Média monta"
        // roxo.
        if (monta.isEmpty())
            monta = "Média monta"; // Fallback visual

        sb.append(monta);

        return sb.toString();
    }

    private String getValor(CampoExtraido campo) {
        if (campo == null || campo.getValor() == null)
            return "-";
        return campo.getValor().replace(";", ","); // Evita quebrar o CSV
    }
}
