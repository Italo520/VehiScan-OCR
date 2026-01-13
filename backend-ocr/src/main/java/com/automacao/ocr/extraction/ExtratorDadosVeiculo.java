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

        // 9. Observações (Busca por palavras-chave críticas e Monta)
        StringBuilder obs = new StringBuilder();
        if (textoParaRegex.toLowerCase().contains("remarcado")) {
            obs.append("Possível Remarcação identificada. ");
        }
        if (textoParaRegex.toLowerCase().contains("sinistro")) {
            obs.append("Indício de Sinistro. ");
        }
        if (textoParaRegex.toLowerCase().contains("leilão") || textoParaRegex.toLowerCase().contains("leilao")) {
            obs.append("Veículo de Leilão. ");
        }

        // Extração de Dano / Monta (CRVSP / Portal Salvados)
        Matcher mMonta = Pattern.compile("(?i)VEIC\\.?\\s*DANO\\s*([^\\n]*MONTA)").matcher(textoParaRegex);
        if (mMonta.find()) {
            String dano = mMonta.group(1).trim();
            if (obs.length() > 0)
                obs.append(" | ");
            obs.append(dano);
            // Salva também em campo específico se quiser mapear para Classificação
            dados.put("Classificação", dano); // Ex: MEDIA MONTA
        }

        // Tenta capturar campo Observações explícito (Multilinha)
        // Procura por "OBSERVAÇÕES DO VEÍCULO" e captura até o próximo bloco de seção
        // ou fim do arquivo
        Pattern pObs = Pattern.compile(
                "(?i)OBSERVA[ÇC][ÕO]ES DO VE[ÍI]CULO[:\\s]*\\n((?:.|\\n)*?)(?=\\n\\s*(?:MENSAGENS|INFORMA[ÇC][ÕO]ES|DADOS|LOCAL|DATA)|$)",
                Pattern.DOTALL);
        Matcher mObs = pObs.matcher(textoParaRegex);
        if (mObs.find()) {
            String conteudoObs = mObs.group(1).trim();
            if (!conteudoObs.isEmpty()) {
                if (obs.length() > 0)
                    obs.append(" | ");
                obs.append(conteudoObs.replaceAll("\\n", " ").replaceAll("\\s+", " "));
            }
        } else {
            // Fallback para formato simples de uma linha
            Pattern pObsSimples = Pattern.compile("(?i)Observa[çc][õo]es[:\\s]+([^\n]+)");
            Matcher mObsSimples = pObsSimples.matcher(textoParaRegex);
            if (mObsSimples.find()) {
                String conteudoObs = mObsSimples.group(1).trim();
                if (!conteudoObs.isEmpty()) {
                    if (obs.length() > 0)
                        obs.append(" | ");
                    obs.append(conteudoObs);
                }
            }
        }

        if (obs.length() > 0) {
            dados.put("Observações", obs.toString().trim());
        }

        // 10. Classificação e Tipo (Lógica de negócio)
        if (textoParaRegex.contains("CERTIFICADO DE REGISTRO")) {
            dados.put("Tipo Documento", "CRV/CRLV");
        } else if (textoParaRegex.contains("CONSULTA CADASTRO DE VEICULO")
                || textoParaRegex.contains("CONSULTA CADASTRO DE VEÍCULO")
                || textoParaRegex.contains("Consulta Base Estadual")) {
            dados.put("Tipo Documento", "CONSULTA DETRAN/SP");
        } else if (textoParaRegex.contains("CERTIDÃO DE BAIXA DO REGISTRO DE VEÍCULO") ||
                textoParaRegex.contains("CERTIDAO DE BAIXA DO REGISTRO DE VEICULO")) {
            dados.put("Tipo Documento", "CERTIDÃO DE BAIXA");

            // Regra de Negócio: Certidão de Baixa -> Grande Monta
            String obsExistente = dados.getOrDefault("Observações", "");
            if (!obsExistente.contains("GRANDE MONTA")) {
                if (!obsExistente.isEmpty())
                    obsExistente += " | ";
                obsExistente += "GRANDE MONTA (Veículo Baixado)";
                dados.put("Observações", obsExistente);
            }
        } else {
            dados.put("Tipo Documento", "Desconhecido");
        }

        // --- Fallbacks / Busca Solta (Loose Match) para CRVSP e layouts tabulares ---

        // Placa Solta (AAA0A00)
        if (!dados.containsKey("Placa")) {
            Matcher mPlacaSolta = Pattern.compile("\\b([A-Z]{3}[0-9][0-9A-Z][0-9]{2})\\b").matcher(textoParaRegex);
            if (mPlacaSolta.find()) {
                dados.put("Placa", TextNormalizer.normalize(mPlacaSolta.group(1)));
            }
        }

        // Chassi Solto (17 chars)
        if (!dados.containsKey("Chassi")) {
            // Evita pegar sequencias numericas muito longas ou IDs, tenta focar em Alphanum
            // 17
            Matcher mChassiSolto = Pattern.compile("\\b([A-Z0-9]{17})\\b").matcher(textoParaRegex);
            while (mChassiSolto.find()) {
                String cand = mChassiSolto.group(1);
                // Validacao basica: nao pode ser so numeros (geralmente chassi tem letras)
                // mas chassi antigo pode ser so numero. O padrao 17 ja filtra bem.
                if (!cand.matches("^\\d+$")) { // Se nao for SO numeros (evita IDs numericos gigantes, embora 17 digitos
                                               // seja chassi msm)
                    dados.put("Chassi", TextNormalizer.normalize(cand));
                    break;
                }
            }
        }

        // Marca/Modelo padrão Detran SP: COD - MARCA/MODELO - COR
        // Ex: 23935 - KAWASAKI/NINJA ZX-6R14 - VERDE
        if (!dados.containsKey("Marca/Modelo") || dados.get("Marca/Modelo").isEmpty()) {
            Matcher mMarcaSP = Pattern.compile("\\d{5,6}\\s*-\\s*([A-Za-z0-9/\\.\\s-]+?)(?:\\s-\\s|$)")
                    .matcher(textoParaRegex);
            if (mMarcaSP.find()) {
                dados.put("Marca/Modelo", TextNormalizer.normalize(mMarcaSP.group(1).trim()));
            }
        }

        // Anos Padrao SP: "2021 2021"
        if (!dados.containsKey("Fabricação")) {
            Matcher mAnosSP = Pattern.compile("\\b((?:19|20)\\d{2})\\s+((?:19|20)\\d{2})\\b").matcher(textoParaRegex);
            if (mAnosSP.find()) {
                dados.put("Fabricação", mAnosSP.group(1));
                dados.put("Ano Modelo", mAnosSP.group(2));
            }
        }

        // Remove valores nulos ou vazios que possam ter entrado
        dados.values().removeIf(val -> val == null || val.trim().isEmpty());

        return dados;
    }
}
