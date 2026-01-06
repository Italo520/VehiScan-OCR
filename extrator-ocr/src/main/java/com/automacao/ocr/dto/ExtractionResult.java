package com.automacao.ocr.dto;

public class ExtractionResult {
    private String value;
    private double confidence;
    private String source; // "REGEX", "LLM", "MANUAL"

    public ExtractionResult() {
    }

    public ExtractionResult(String value, double confidence, String source) {
        this.value = value;
        this.confidence = confidence;
        this.source = source;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
