package com.automacao.ocr.pipeline;

public interface Validator {
    boolean isValid(String value);

    String refine(String value);
}
