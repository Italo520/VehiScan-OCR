package com.automacao.auditoria.controller;

import com.automacao.auditoria.component.DocumentoListCell;
import com.automacao.auditoria.service.MongoService;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.StatusExtracao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewController {

    @FXML
    private ListView<DocumentoVeiculoDTO> listViewDocumentos;
    @FXML
    private TextField txtSearch;
    @FXML
    private Label lblDocumentoId;
    @FXML
    private ImageView imageView;

    // Dados do Veículo
    @FXML
    private TextField txtPlaca;
    @FXML
    private TextField txtChassi;
    @FXML
    private TextField txtModelo;
    @FXML
    private TextField txtMarca;
    @FXML
    private TextField txtAnoFab;
    @FXML
    private TextField txtRenavam;
    @FXML
    private TextField txtCpfCnpj;

    // Dados FIPE
    @FXML
    private TextField txtFipeCodigo;
    @FXML
    private TextField txtFipeMarca;
    @FXML
    private TextField txtFipeModelo;
    @FXML
    private TextField txtFipeAno;
    @FXML
    private TextField txtFipePreco;

    @FXML
    private CheckBox chkNecessitaRevisao;
    @FXML
    private TextArea txtOcrRaw;
    @FXML
    private TextArea txtLog;

    private DocumentoVeiculoDTO documentoAtual;
    private MongoService mongoService;
    private ObservableList<DocumentoVeiculoDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<DocumentoVeiculoDTO> filteredData;

    public void initialize() {
        mongoService = new MongoService();

        // Configurar Lista
        listViewDocumentos.setCellFactory(param -> new DocumentoListCell());
        listViewDocumentos.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setDocumento(newValue);
        });

        // Configurar Busca
        filteredData = new FilteredList<>(masterData, p -> true);
        listViewDocumentos.setItems(filteredData);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(doc -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (doc.getPlaca() != null && doc.getPlaca().getValor() != null &&
                        doc.getPlaca().getValor().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (doc.getChassi() != null && doc.getChassi().getValor() != null &&
                        doc.getChassi().getValor().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        atualizarLista();
    }

    @FXML
    public void atualizarLista() {
        if (mongoService.isConnected()) {
            List<DocumentoVeiculoDTO> pendentes = mongoService.buscarPendentes();
            masterData.setAll(pendentes);
            if (!masterData.isEmpty()) {
                listViewDocumentos.getSelectionModel().selectFirst();
            }
        } else {
            showAlert("Erro de Conexão", "Não foi possível conectar ao MongoDB.");
        }
    }

    public void setDocumento(DocumentoVeiculoDTO doc) {
        this.documentoAtual = doc;
        if (doc == null) {
            limparCampos();
            return;
        }

        preencherCampos();
        // TODO: Carregar imagem real
        // imageView.setImage(new Image(doc.getCaminhoImagem()));
    }

    private void limparCampos() {
        lblDocumentoId.setText("ID: ...");
        txtPlaca.clear();
        txtChassi.clear();
        txtModelo.clear();
        txtMarca.clear();
        txtAnoFab.clear();
        txtRenavam.clear();
        txtCpfCnpj.clear();
        txtFipeCodigo.clear();
        txtFipeMarca.clear();
        txtFipeModelo.clear();
        txtFipeAno.clear();
        txtFipePreco.clear();
        chkNecessitaRevisao.setSelected(false);
        txtOcrRaw.clear();
        txtLog.clear();
        imageView.setImage(null);
    }

    private void preencherCampos() {
        if (documentoAtual == null)
            return;

        // Tentar usar placa ou chassi como ID visual
        String idVisual = "N/A";
        if (documentoAtual.getPlaca() != null && documentoAtual.getPlaca().getValor() != null) {
            idVisual = documentoAtual.getPlaca().getValor();
        } else if (documentoAtual.getChassi() != null) {
            idVisual = documentoAtual.getChassi().getValor();
        }
        lblDocumentoId.setText("ID: " + idVisual);

        setCampo(txtPlaca, documentoAtual.getPlaca());
        setCampo(txtChassi, documentoAtual.getChassi());
        setCampo(txtModelo, documentoAtual.getModelo());
        setCampo(txtMarca, documentoAtual.getMarca());
        setCampo(txtAnoFab, documentoAtual.getFabricacao());
        setCampo(txtRenavam, documentoAtual.getRenavam());
        setCampo(txtCpfCnpj, documentoAtual.getCpfCnpj());

        // Preencher dados FIPE
        if (documentoAtual.getDadosFipe() != null) {
            com.automacao.ocr.fipe.dto.ValorFipeDTO fipe = documentoAtual.getDadosFipe();
            txtFipeCodigo.setText(fipe.codigoFipe);
            txtFipeMarca.setText(fipe.marca);
            txtFipeModelo.setText(fipe.modelo);
            txtFipeAno.setText(fipe.anoModelo != null ? fipe.anoModelo.toString() : "");
            txtFipePreco.setText(fipe.valor);
        } else {
            txtFipeCodigo.setText("");
            txtFipeMarca.setText("");
            txtFipeModelo.setText("");
            txtFipeAno.setText("");
            txtFipePreco.setText("");
        }

        chkNecessitaRevisao.setSelected(documentoAtual.isNecessitaRevisao());

        // Logs
        if (documentoAtual.getAuditoria() != null) {
            String logs = documentoAtual.getAuditoria().stream()
                    .map(log -> String.format("%s - %s: %s -> %s (%s)",
                            log.getDataHora(),
                            log.getCampo(),
                            log.getValorAnterior(),
                            log.getValorNovo(),
                            log.getUsuario()))
                    .collect(Collectors.joining("\n"));
            txtLog.setText(logs);
        } else {
            txtLog.setText("");
        }
    }

    private void setCampo(TextField txt, CampoExtraido campo) {
        if (campo != null) {
            txt.setText(campo.getValor());
            // Highlight low confidence
            if (campo.getConfianca() < 0.8) {
                txt.setStyle("-fx-background-color: #FFF9C4;"); // Light yellow
            } else {
                txt.setStyle("");
            }
        } else {
            txt.setText("");
            txt.setStyle("");
        }
    }

    @FXML
    private void salvar() {
        if (documentoAtual == null)
            return;

        atualizarDTO();
        mongoService.salvarDocumento(documentoAtual);
        showAlert("Sucesso", "Alterações salvas com sucesso.");
        listViewDocumentos.refresh(); // Atualizar visual da lista
    }

    @FXML
    private void marcarRevisado() {
        if (documentoAtual == null)
            return;

        atualizarDTO();
        documentoAtual.setNecessitaRevisao(false);
        documentoAtual.setStatusExtracao(StatusExtracao.COMPLETO);
        mongoService.salvarDocumento(documentoAtual);
        showAlert("Sucesso", "Documento aprovado e marcado como revisado.");

        // Remover da lista ou atualizar
        masterData.remove(documentoAtual);
        listViewDocumentos.getSelectionModel().clearSelection();
        if (!masterData.isEmpty()) {
            listViewDocumentos.getSelectionModel().selectFirst();
        } else {
            limparCampos();
        }
    }

    @FXML
    private void voltar() {
        // Deprecated in new layout
    }

    private void atualizarDTO() {
        if (documentoAtual == null)
            return;

        if (documentoAtual.getPlaca() != null)
            documentoAtual.getPlaca().setValor(txtPlaca.getText());
        if (documentoAtual.getChassi() != null)
            documentoAtual.getChassi().setValor(txtChassi.getText());
        if (documentoAtual.getModelo() != null)
            documentoAtual.getModelo().setValor(txtModelo.getText());
        if (documentoAtual.getMarca() != null)
            documentoAtual.getMarca().setValor(txtMarca.getText());
        if (documentoAtual.getFabricacao() != null)
            documentoAtual.getFabricacao().setValor(txtAnoFab.getText());
        if (documentoAtual.getRenavam() != null)
            documentoAtual.getRenavam().setValor(txtRenavam.getText());
        if (documentoAtual.getCpfCnpj() != null)
            documentoAtual.getCpfCnpj().setValor(txtCpfCnpj.getText());

        // Atualizar dados FIPE
        if (documentoAtual.getDadosFipe() == null) {
            documentoAtual.setDadosFipe(new com.automacao.ocr.fipe.dto.ValorFipeDTO());
        }
        com.automacao.ocr.fipe.dto.ValorFipeDTO fipe = documentoAtual.getDadosFipe();
        fipe.codigoFipe = txtFipeCodigo.getText();
        fipe.marca = txtFipeMarca.getText();
        fipe.modelo = txtFipeModelo.getText();
        try {
            if (!txtFipeAno.getText().isEmpty()) {
                fipe.anoModelo = Integer.parseInt(txtFipeAno.getText());
            } else {
                fipe.anoModelo = null;
            }
        } catch (NumberFormatException e) {
            // Ignore invalid number
        }
        fipe.valor = txtFipePreco.getText();

        documentoAtual.setNecessitaRevisao(chkNecessitaRevisao.isSelected());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
