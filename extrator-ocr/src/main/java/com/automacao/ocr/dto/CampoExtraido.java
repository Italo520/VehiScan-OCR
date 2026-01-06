package com.automacao.ocr.dto;

public class CampoExtraido {
    private String valor;
    private CampoStatus status;
    private String motivo;
    private double confianca;
    private String origem; // "REGEX", "LLM", "MANUAL"
    private boolean validadoManualmente;

    public CampoExtraido() {
    }

    public CampoExtraido(String valor, CampoStatus status, String motivo) {
        this(valor, status, motivo, 1.0, "DESCONHECIDO");
    }

    public CampoExtraido(String valor, CampoStatus status, String motivo, double confianca, String origem) {
        this.valor = valor;
        this.status = status;
        this.motivo = motivo;
        this.confianca = confianca;
        this.origem = origem;
        this.validadoManualmente = false;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public CampoStatus getStatus() {
        return status;
    }

    public void setStatus(CampoStatus status) {
        this.status = status;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getConfianca() {
        return confianca;
    }

    public void setConfianca(double confianca) {
        this.confianca = confianca;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public boolean isValidadoManualmente() {
        return validadoManualmente;
    }

    public void setValidadoManualmente(boolean validadoManualmente) {
        this.validadoManualmente = validadoManualmente;
    }

    @Override
    public String toString() {
        return String.format("Valor='%s', Status=%s, Motivo='%s', Conf=%.2f, Origem='%s'",
                valor, status, motivo, confianca, origem);
    }
}
