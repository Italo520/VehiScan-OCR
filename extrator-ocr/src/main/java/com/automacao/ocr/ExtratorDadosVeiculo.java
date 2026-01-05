package com.automacao.ocr;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtratorDadosVeiculo {

    // Regex ajustados para suportar variações (Consulta, CRLV, etc)
    // (?i) torna case-insensitive
    private static final Pattern PATTERN_PLACA = Pattern
            .compile("(?i)(?:Nº da )?Placa[:\\s]+([A-Z]{3}[0-9][0-9A-Z][0-9]{2})");
    // Chassi: aceita "Chassi" ou "Chassi REM" (remarcado), seguido de : ou espaço,
    // e o código
    private static final Pattern PATTERN_CHASSI = Pattern.compile("(?i)(?:Chassi|Chassi\\s+REM)[:\\s]+([A-Z0-9]{17})");
    // Ano: aceita "Ano Fabricação", "Ano Fabr.", "Ano Fabr"
    private static final Pattern PATTERN_ANO_FAB = Pattern.compile("(?i)Ano Fab(?:rica[çc][ãa]o|r\\.?)[:\\s]+(\\d{4})");
    private static final Pattern PATTERN_ANO_MOD = Pattern.compile("(?i)Ano Modelo[:\\s]+(\\d{4})");
    // Marca: pega até o fim da linha
    private static final Pattern PATTERN_MARCA = Pattern.compile("(?i)Marca[:\\s]+([^\n]+)");

    public Map<String, String> extrairDados(String textoBruto) {
        Map<String, String> dados = new HashMap<>();

        // Normaliza quebras de linha para facilitar regex
        String texto = textoBruto.replaceAll("\\r\\n", "\n");

        // 1. Placa
        Matcher mPlaca = PATTERN_PLACA.matcher(texto);
        if (mPlaca.find()) {
            dados.put("Placa", mPlaca.group(1));
        }

        // 2. Chassi
        Matcher mChassi = PATTERN_CHASSI.matcher(texto);
        if (mChassi.find()) {
            dados.put("Chassi", mChassi.group(1));
        }

        // 3. Ano Fabricação
        Matcher mAnoFab = PATTERN_ANO_FAB.matcher(texto);
        if (mAnoFab.find()) {
            dados.put("Fabricação", mAnoFab.group(1));
        }

        // 4. Ano Modelo
        Matcher mAnoMod = PATTERN_ANO_MOD.matcher(texto);
        if (mAnoMod.find()) {
            dados.put("Ano Modelo", mAnoMod.group(1));
        }

        // 5. Marca/Modelo
        Matcher mMarca = PATTERN_MARCA.matcher(texto);
        if (mMarca.find()) {
            String marcaCompleta = mMarca.group(1).trim();
            // Remove código numérico inicial se existir (ex: "149522 - GM/CELTA")
            if (marcaCompleta.matches("^\\d+\\s*-\\s*.*")) {
                int indexTraco = marcaCompleta.indexOf("-");
                if (indexTraco >= 0 && indexTraco < marcaCompleta.length() - 1) {
                    marcaCompleta = marcaCompleta.substring(indexTraco + 1).trim();
                }
            }
            dados.put("Marca/Modelo", marcaCompleta);
        }

        // 6. Classificação e Tipo (Lógica de negócio)
        if (texto.contains("CERTIFICADO DE REGISTRO")) {
            dados.put("Tipo Documento", "CRV/CRLV");
        } else if (texto.contains("CONSULTA CADASTRO DE VEICULO") || texto.contains("CONSULTA CADASTRO DE VEÍCULO")) {
            dados.put("Tipo Documento", "CONSULTA CADASTRO DE VEICULO");
        } else {
            dados.put("Tipo Documento", "Desconhecido");
        }

        return dados;
    }
}
