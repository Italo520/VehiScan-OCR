package com.automacao.ocr.pipeline.validators;

import com.automacao.ocr.pipeline.Validator;

public class RenavamValidator implements Validator {

    @Override
    public boolean isValid(String value) {
        if (value == null)
            return false;
        String limpo = value.replaceAll("[^0-9]", "");
        // Renavam tem 11 dígitos (atualmente)
        if (limpo.length() != 11)
            return false;

        return validarDigitoVerificador(limpo);
    }

    @Override
    public String refine(String value) {
        if (value == null)
            return null;
        return value.replaceAll("[^0-9]", "");
    }

    private boolean validarDigitoVerificador(String renavam) {
        // Algoritmo de validação do Renavam (simplificado ou completo)
        // Implementação completa do algoritmo Modulo 11
        // Fonte: Comum em validações de documentos brasileiros

        // Se todos os dígitos forem iguais, é inválido
        if (renavam.matches("^(\\d)\\1*$"))
            return false;

        String renavamSemDigito = renavam.substring(0, 10);
        String digitoInformado = renavam.substring(10);

        int soma = 0;
        int[] pesos = { 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(renavamSemDigito.charAt(i)) * pesos[i];
        }

        int resto = soma % 11;
        int digitoCalculado = 11 - resto;
        if (digitoCalculado >= 10) {
            digitoCalculado = 0;
        }

        return String.valueOf(digitoCalculado).equals(digitoInformado);
    }
}
