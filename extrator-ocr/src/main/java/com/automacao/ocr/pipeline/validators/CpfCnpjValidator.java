package com.automacao.ocr.pipeline.validators;

import com.automacao.ocr.pipeline.Validator;

public class CpfCnpjValidator implements Validator {

    @Override
    public boolean isValid(String value) {
        if (value == null)
            return false;
        String limpo = value.replaceAll("[^0-9]", "");

        if (limpo.length() == 11) {
            return isCpf(limpo);
        } else if (limpo.length() == 14) {
            return isCnpj(limpo);
        }
        return false;
    }

    @Override
    public String refine(String value) {
        if (value == null)
            return null;
        return value.replaceAll("[^0-9]", "");
    }

    private boolean isCpf(String cpf) {
        if (cpf.matches("^(\\d)\\1*$"))
            return false;

        try {
            int soma = 0;
            int peso = 10;
            for (int i = 0; i < 9; i++) {
                soma += (cpf.charAt(i) - '0') * peso--;
            }

            int r = 11 - (soma % 11);
            char dig10 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            soma = 0;
            peso = 11;
            for (int i = 0; i < 10; i++) {
                soma += (cpf.charAt(i) - '0') * peso--;
            }

            r = 11 - (soma % 11);
            char dig11 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            return (dig10 == cpf.charAt(9)) && (dig11 == cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCnpj(String cnpj) {
        if (cnpj.matches("^(\\d)\\1*$"))
            return false;

        try {
            int soma = 0, peso = 2;
            for (int i = 11; i >= 0; i--) {
                soma += (cnpj.charAt(i) - '0') * peso;
                peso = (peso == 9) ? 2 : peso + 1;
            }
            int r = soma % 11;
            char dig13 = (r < 2) ? '0' : (char) ((11 - r) + '0');

            soma = 0;
            peso = 2;
            for (int i = 12; i >= 0; i--) {
                soma += (cnpj.charAt(i) - '0') * peso;
                peso = (peso == 9) ? 2 : peso + 1;
            }
            r = soma % 11;
            char dig14 = (r < 2) ? '0' : (char) ((11 - r) + '0');

            return (dig13 == cnpj.charAt(12)) && (dig14 == cnpj.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }
}
