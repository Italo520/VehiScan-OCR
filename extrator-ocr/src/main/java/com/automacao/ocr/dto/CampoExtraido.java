package com.automacao.ocr.dto;

public class CampoExtraido {
    private String valor;
    private CampoStatus status;
    private String motivo;

    public CampoExtraido() {
    }

    public CampoExtraido(String valor, CampoStatus status, String motivo) {
        this.valor = valor;
        this.status = status;
        this.motivo = motivo;
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

    @Override
    public String toString() {
        return String.format("Valor='%s', Status=%s, Motivo='%s'", valor, status, motivo);
    }
}
