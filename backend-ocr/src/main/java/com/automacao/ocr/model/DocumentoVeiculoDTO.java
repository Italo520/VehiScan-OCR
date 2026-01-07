package com.automacao.ocr.model;

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
    private CampoExtraido renavam;
    private CampoExtraido cpfCnpj;
    private CampoExtraido nomeProprietario;

    // Novos campos de auditoria
    private StatusExtracao statusExtracao;
    private boolean necessitaRevisao;
    private java.util.List<AuditLog> auditoria;

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
        this.renavam = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.cpfCnpj = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");
        this.nomeProprietario = new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Inicializado");

        this.statusExtracao = StatusExtracao.PENDENTE;
        this.necessitaRevisao = true;
        this.auditoria = new java.util.ArrayList<>();
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

    public CampoExtraido getRenavam() {
        return renavam;
    }

    public void setRenavam(CampoExtraido renavam) {
        this.renavam = renavam;
    }

    public CampoExtraido getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(CampoExtraido cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public CampoExtraido getNomeProprietario() {
        return nomeProprietario;
    }

    public void setNomeProprietario(CampoExtraido nomeProprietario) {
        this.nomeProprietario = nomeProprietario;
    }

    public StatusExtracao getStatusExtracao() {
        return statusExtracao;
    }

    public void setStatusExtracao(StatusExtracao statusExtracao) {
        this.statusExtracao = statusExtracao;
    }

    public boolean isNecessitaRevisao() {
        return necessitaRevisao;
    }

    public void setNecessitaRevisao(boolean necessitaRevisao) {
        this.necessitaRevisao = necessitaRevisao;
    }

    public java.util.List<AuditLog> getAuditoria() {
        return auditoria;
    }

    public void setAuditoria(java.util.List<AuditLog> auditoria) {
        this.auditoria = auditoria;
    }

    private String caminhoArquivo;

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }
}
