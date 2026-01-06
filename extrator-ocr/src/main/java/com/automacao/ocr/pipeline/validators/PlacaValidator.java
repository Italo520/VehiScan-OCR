package com.automacao.ocr.pipeline.validators;

import com.automacao.ocr.pipeline.Validator;
import java.util.regex.Pattern;

public class PlacaValidator implements Validator {

    // Padrão Mercosul: AAA1A11 ou Antigo: AAA1234
    // Regex genérico para ambos: [A-Z]{3}[0-9][0-9A-Z][0-9]{2}
    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}[0-9][0-9A-Z][0-9]{2}$");

    @Override
    public boolean isValid(String value) {
        if (value == null)
            return false;
        // Remove hífens e espaços para validar
        String limpo = value.replace("-", "").replace(" ", "").toUpperCase();
        return PATTERN.matcher(limpo).matches();
    }

    @Override
    public String refine(String value) {
        if (value == null)
            return null;
        return value.replace("-", "").replace(" ", "").toUpperCase();
    }
}
