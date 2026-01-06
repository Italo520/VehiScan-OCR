package com.automacao.ocr.ocr;

import java.util.Map;

public class ResultadoExtracaoTexto {
    private TipoDocumentoFonte tipoFonte;
    private String textoCompleto;
    private Map<Integer, String> textoPorPagina; // opcional, p/ debugging

    public TipoDocumentoFonte getTipoFonte() {
        return tipoFonte;
    }

    public void setTipoFonte(TipoDocumentoFonte tipoFonte) {
        this.tipoFonte = tipoFonte;
    }

    public String getTextoCompleto() {
        return textoCompleto;
    }

    public void setTextoCompleto(String textoCompleto) {
        this.textoCompleto = textoCompleto;
    }

    public Map<Integer, String> getTextoPorPagina() {
        return textoPorPagina;
    }

    public void setTextoPorPagina(Map<Integer, String> textoPorPagina) {
        this.textoPorPagina = textoPorPagina;
    }
}
