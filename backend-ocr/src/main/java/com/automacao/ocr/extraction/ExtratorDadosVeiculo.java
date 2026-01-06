package com.automacao.ocr.extraction;

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

    // Novos padrões
    private static final Pattern PATTERN_RENAVAM = Pattern.compile("(?i)Renavam[:\\s]+([0-9]{9,11})");
    private static final Pattern PATTERN_CPF_CNPJ = Pattern.compile("(?i)(?:CPF|CNPJ)[:\\s]+([0-9./-]+)");
    private static final Pattern PATTERN_NOME = Pattern.compile("(?i)Nome[:\\s]+([^\n]+)");

    public Map<String, String> extrairDados(String textoBruto) {
        Map<String, String> dados = new HashMap<>();

        // O normalizer remove acentos e converte para upper, mas o regex original
        // esperava acentos em alguns casos (ex: Fabricação)
        // Como o normalizer remove acentos, precisamos ajustar os regex ou usar o texto
        // original para regex que dependem de acento.
        // O normalizer também remove quebras de linha duplicadas e converte para \n.
        // Vamos usar uma versão levemente normalizada para regex que preserva estrutura
        // mas limpa ruído.

        // Para compatibilidade com regex existentes que esperam acentos (ex:
        // "Fabricação"),
        // vamos usar o textoBruto com replace básico de linhas, mas aplicar
        // normalização nos valores extraídos.
        String textoParaRegex = textoBruto.replaceAll("\\r\\n", "\n");

        // 1. Placa
        Matcher mPlaca = PATTERN_PLACA.matcher(textoParaRegex);
        if (mPlaca.find()) {
            dados.put("Placa", TextNormalizer.normalize(mPlaca.group(1)));
        }

        // 2. Chassi
        Matcher mChassi = PATTERN_CHASSI.matcher(textoParaRegex);
        if (mChassi.find()) {
            dados.put("Chassi", TextNormalizer.normalize(mChassi.group(1)));
        }

        // 3. Ano Fabricação
        Matcher mAnoFab = PATTERN_ANO_FAB.matcher(textoParaRegex);
        if (mAnoFab.find()) {
            dados.put("Fabricação", mAnoFab.group(1));
        }

        // 4. Ano Modelo
        Matcher mAnoMod = PATTERN_ANO_MOD.matcher(textoParaRegex);
        if (mAnoMod.find()) {
            dados.put("Ano Modelo", mAnoMod.group(1));
        }

        // 5. Marca/Modelo
        Matcher mMarca = PATTERN_MARCA.matcher(textoParaRegex);
        while (mMarca.find()) {
            String marcaCompleta = mMarca.group(1).trim();

            // Ignora se for cabeçalho (ex: "Marca / Modelo / Versão")
            if (marcaCompleta.toUpperCase().contains("MODELO") && marcaCompleta.toUpperCase().contains("VERS")) {
                continue;
            }

            // Remove código numérico inicial se existir (ex: "149522 - GM/CELTA")
            if (marcaCompleta.matches("^\\d+\\s*-\\s*.*")) {
                int indexTraco = marcaCompleta.indexOf("-");
                if (indexTraco >= 0 && indexTraco < marcaCompleta.length() - 1) {
                    marcaCompleta = marcaCompleta.substring(indexTraco + 1).trim();
                }
            }
            dados.put("Marca/Modelo", TextNormalizer.normalize(marcaCompleta));
            break; // Encontrou um valor válido, para.
        }

        // 6. Renavam
        Matcher mRenavam = PATTERN_RENAVAM.matcher(textoParaRegex);
        if (mRenavam.find()) {
            dados.put("Renavam", mRenavam.group(1));
        }

        // 7. CPF/CNPJ
        Matcher mCpfCnpj = PATTERN_CPF_CNPJ.matcher(textoParaRegex);
        if (mCpfCnpj.find()) {
            dados.put("CPF/CNPJ", mCpfCnpj.group(1));
        }

        // 8. Nome Proprietário
        Matcher mNome = PATTERN_NOME.matcher(textoParaRegex);
        if (mNome.find()) {
            dados.put("Nome", TextNormalizer.normalize(mNome.group(1)));
        }

        // 9. Classificação e Tipo (Lógica de negócio)
        if (textoParaRegex.contains("CERTIFICADO DE REGISTRO")) {
            dados.put("Tipo Documento", "CRV/CRLV");
        } else if (textoParaRegex.contains("CONSULTA CADASTRO DE VEICULO")
                || textoParaRegex.contains("CONSULTA CADASTRO DE VEÍCULO")) {
            dados.put("Tipo Documento", "CONSULTA CADASTRO DE VEICULO");
        } else {
            dados.put("Tipo Documento", "Desconhecido");
        }

        return dados;
    }
}
