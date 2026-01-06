package com.automacao.ocr.extraction;

public interface Validator {
    boolean isValid(String value);

    String refine(String value);
}
