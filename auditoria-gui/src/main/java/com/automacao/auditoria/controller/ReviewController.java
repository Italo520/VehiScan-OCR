package com.automacao.auditoria.controller;

import com.automacao.auditoria.AuditoriaApp;
import com.automacao.auditoria.service.MongoService;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.StatusExtracao;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class ReviewController {

    @FXML
    private Label lblDocumentoId;
    @FXML
    private ImageView imageView;
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

    private DocumentoVeiculoDTO documento;
    private MongoService mongoService;

    public void initialize() {
        mongoService = new MongoService();
    }

    public void setDocumento(DocumentoVeiculoDTO doc) {
        this.documento = doc;
        preencherCampos();
    }

    private void preencherCampos() {
        if (documento == null)
            return;

        // TODO: Set ID in label if available

        setCampo(txtPlaca, documento.getPlaca());
        setCampo(txtChassi, documento.getChassi());
        setCampo(txtModelo, documento.getModelo());
        setCampo(txtMarca, documento.getMarca());
        setCampo(txtAnoFab, documento.getFabricacao());
        setCampo(txtRenavam, documento.getRenavam());
        setCampo(txtCpfCnpj, documento.getCpfCnpj());

        // Preencher dados FIPE
        if (documento.getDadosFipe() != null) {
            com.automacao.ocr.fipe.dto.ValorFipeDTO fipe = documento.getDadosFipe();
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

        chkNecessitaRevisao.setSelected(documento.isNecessitaRevisao());

        // TODO: Set OCR raw text and logs if available in DTO
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
        }
    }

    @FXML
    private void salvar() {
        atualizarDTO();
        mongoService.salvarDocumento(documento);
        showAlert("Sucesso", "Alterações salvas com sucesso.");
    }

    @FXML
    private void marcarRevisado() {
        atualizarDTO();
        documento.setNecessitaRevisao(false);
        documento.setStatusExtracao(StatusExtracao.COMPLETO);
        mongoService.salvarDocumento(documento);
        showAlert("Sucesso", "Documento marcado como revisado.");
        voltar();
    }

    private void atualizarDTO() {
        if (documento.getPlaca() != null)
            documento.getPlaca().setValor(txtPlaca.getText());
        if (documento.getChassi() != null)
            documento.getChassi().setValor(txtChassi.getText());
        if (documento.getModelo() != null)
            documento.getModelo().setValor(txtModelo.getText());
        if (documento.getMarca() != null)
            documento.getMarca().setValor(txtMarca.getText());
        if (documento.getFabricacao() != null)
            documento.getFabricacao().setValor(txtAnoFab.getText());
        if (documento.getRenavam() != null)
            documento.getRenavam().setValor(txtRenavam.getText());
        if (documento.getCpfCnpj() != null)
            documento.getCpfCnpj().setValor(txtCpfCnpj.getText());

        // Atualizar dados FIPE
        if (documento.getDadosFipe() == null) {
            documento.setDadosFipe(new com.automacao.ocr.fipe.dto.ValorFipeDTO());
        }
        com.automacao.ocr.fipe.dto.ValorFipeDTO fipe = documento.getDadosFipe();
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

        documento.setNecessitaRevisao(chkNecessitaRevisao.isSelected());
    }

    @FXML
    private void voltar() {
        try {
            // This is a bit hacky, ideally we use a navigation service
            // We need to reload the list view
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/document_list.fxml"));
            javafx.scene.Parent root = loader.load();
            lblDocumentoId.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
