package com.automacao.ocr.dto;

public class DocumentoVeiculoDTO {
    private CampoExtraido tipoDocumento;
    private CampoExtraido placa;
    private CampoExtraido marca;
    private CampoExtraido fabricacao;
    private CampoExtraido modelo;
    private CampoExtraido chassi;
    private CampoExtraido classificacao;
    private CampoExtraido observacoes;
    private com.automacao.ocr.fipe.dto.ValorFipeDTO dadosFipe;

    public DocumentoVeiculoDTO() {
        // Inicializa com status NAO_ENCONTRADO por padrão para evitar NullPointer
        this.tipoDocumento = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.placa = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.marca = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.fabricacao = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.modelo = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.chassi = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.classificacao = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.observacoes = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
    }

    public CampoExtraido getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(CampoExtraido tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public CampoExtraido getPlaca() {
        return placa;
    }

    public void setPlaca(CampoExtraido placa) {
        this.placa = placa;
    }

    public CampoExtraido getMarca() {
        return marca;
    }

    public void setMarca(CampoExtraido marca) {
        this.marca = marca;
    }

    public CampoExtraido getFabricacao() {
        return fabricacao;
    }

    public void setFabricacao(CampoExtraido fabricacao) {
        this.fabricacao = fabricacao;
    }

    public CampoExtraido getModelo() {
        return modelo;
    }

    public void setModelo(CampoExtraido modelo) {
        this.modelo = modelo;
    }

    public CampoExtraido getChassi() {
        return chassi;
    }

    public void setChassi(CampoExtraido chassi) {
        this.chassi = chassi;
    }

    public CampoExtraido getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(CampoExtraido classificacao) {
        this.classificacao = classificacao;
    }

    public CampoExtraido getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(CampoExtraido observacoes) {
        this.observacoes = observacoes;
    }

    public com.automacao.ocr.fipe.dto.ValorFipeDTO getDadosFipe() {
        return dadosFipe;
    }

    public void setDadosFipe(com.automacao.ocr.fipe.dto.ValorFipeDTO dadosFipe) {
        this.dadosFipe = dadosFipe;
    }
}
