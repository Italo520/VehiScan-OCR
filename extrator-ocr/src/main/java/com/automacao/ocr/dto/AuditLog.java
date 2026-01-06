package com.automacao.ocr.dto;

import java.time.LocalDateTime;

public class AuditLog {
    private String campo;
    private String valorAnterior;
    private String valorNovo;
    private String usuario;
    private LocalDateTime dataHora;

    public AuditLog() {
    }

    public AuditLog(String campo, String valorAnterior, String valorNovo, String usuario) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.usuario = usuario;
        this.dataHora = LocalDateTime.now();
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(String valorNovo) {
        this.valorNovo = valorNovo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
