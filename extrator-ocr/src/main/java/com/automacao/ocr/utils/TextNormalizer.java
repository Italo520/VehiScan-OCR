package com.automacao.ocr.utils;

import java.text.Normalizer;

public class TextNormalizer {

    public static String normalize(String text) {
        if (text == null)
            return null;

        // Remove acentos
        String semAcentos = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove caracteres de controle e não imprimíveis (exceto newline)
        String limpo = semAcentos.replaceAll("[^\\x20-\\x7E\\n]", "");

        // Remove espaços duplicados
        limpo = limpo.replaceAll("\\s+", " ").trim();

        return limpo.toUpperCase();
    }

    public static String fixCommonOcrErrors(String text) {
        if (text == null)
            return null;

        // Exemplo: 0 vs O em contextos numéricos (muito difícil de generalizar sem
        // contexto,
        // mas podemos fazer substituições seguras se soubermos que é um campo
        // específico).
        // Aqui faremos apenas limpezas gerais se houver.

        return text;
    }

    // Método específico para corrigir placas/chassis onde 0 e O se confundem
    public static String fixAlphaNumericConfusion(String text) {
        if (text == null)
            return null;
        // Substituições comuns
        // 5 -> S (se parecer letra)
        // S -> 5 (se parecer número)
        // Isso depende muito do contexto (se esperamos letra ou número).
        // Deixaremos para o validador/refinador específico.
        return text;
    }
}
