package com.automacao.ocr.pipeline.validators;

import com.automacao.ocr.pipeline.Validator;
import java.util.regex.Pattern;

public class ChassiValidator implements Validator {

    // Chassi tem 17 caracteres alfanuméricos (exceto I, O, Q para evitar confusão,
    // mas na prática regex aceita tudo e valida tamanho)
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9]{17}$");

    @Override
    public boolean isValid(String value) {
        if (value == null)
            return false;
        String limpo = value.replace(" ", "").replace("-", "").toUpperCase();
        // Correção básica de O/0 e I/1 poderia ser feita aqui ou no Normalizer.
        // Vamos assumir que o Normalizer faz o grosso, mas aqui garantimos formato.
        return PATTERN.matcher(limpo).matches();
    }

    @Override
    public String refine(String value) {
        if (value == null)
            return null;
        return value.replace(" ", "").replace("-", "").toUpperCase();
    }
}
