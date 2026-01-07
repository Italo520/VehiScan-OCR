package com.automacao.auditoria.controller;

import com.automacao.auditoria.component.DocumentoListCell;
import com.automacao.auditoria.service.MongoService;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.StatusExtracao;
import com.automacao.ocr.model.CampoStatus;
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

    @FXML
    private Label lblPastaEntrada;
    @FXML
    private RadioButton rbMongoOnly;
    @FXML
    private RadioButton rbMongoCsv;
    @FXML
    private TextField txtPastaSaida;
    @FXML
    private Button btnSelecionarSaida;
    @FXML
    private Label lblStatusConexao;
    @FXML
    private Label lblStatusPastaEntrada;
    @FXML
    private Label lblStatusPastaSaida;

    private DocumentoVeiculoDTO documentoAtual;
    private MongoService mongoService;
    private com.automacao.auditoria.service.LocalFileService localFileService;
    private ObservableList<DocumentoVeiculoDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<DocumentoVeiculoDTO> filteredData;
    private java.io.File pastaEntradaAtual;
    private java.io.File pastaSaidaAtual;

    public void initialize() {
        mongoService = new MongoService();
        localFileService = new com.automacao.auditoria.service.LocalFileService();

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

        // Configurar RadioButtons
        ToggleGroup group = new ToggleGroup();
        rbMongoOnly.setToggleGroup(group);
        rbMongoCsv.setToggleGroup(group);

        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            boolean exportarCsv = rbMongoCsv.isSelected();
            txtPastaSaida.setDisable(!exportarCsv);
            btnSelecionarSaida.setDisable(!exportarCsv);
        });

        atualizarStatus();
        atualizarLista();
    }

    @FXML
    public void atualizarLista() {
        masterData.clear();

        // 1. Carregar do MongoDB
        if (mongoService.isConnected()) {
            List<DocumentoVeiculoDTO> pendentes = mongoService.buscarPendentes();
            masterData.addAll(pendentes);
        }

        // 2. Carregar da pasta local (se selecionada)
        if (pastaEntradaAtual != null) {
            List<java.io.File> arquivos = localFileService.listarArquivos(pastaEntradaAtual);
            for (java.io.File arquivo : arquivos) {
                DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();
                dto.setPlaca(new CampoExtraido(arquivo.getName(), CampoStatus.OK, "Arquivo Local", 1.0, "MANUAL"));
                dto.setNecessitaRevisao(true);
                masterData.add(dto);
            }
        }

        if (!masterData.isEmpty()) {
            listViewDocumentos.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void carregarArquivo() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Carregar Documento");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Imagens e PDFs", "*.jpg", "*.jpeg", "*.png", "*.pdf"));
        java.io.File file = fileChooser.showOpenDialog(listViewDocumentos.getScene().getWindow());
        if (file != null) {
            DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();
            dto.setPlaca(new CampoExtraido(file.getName(), CampoStatus.OK, "Arquivo Local", 1.0, "MANUAL"));
            dto.setNecessitaRevisao(true);
            masterData.add(0, dto);
            listViewDocumentos.getSelectionModel().select(dto);
        }
    }

    @FXML
    private void selecionarPastaEntrada() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Selecionar Pasta de Entrada");
        java.io.File selectedDirectory = directoryChooser.showDialog(listViewDocumentos.getScene().getWindow());
        if (selectedDirectory != null) {
            pastaEntradaAtual = selectedDirectory;
            lblPastaEntrada.setText(selectedDirectory.getAbsolutePath());
            atualizarStatus();
            atualizarLista();
        }
    }

    @FXML
    private void selecionarPastaSaida() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Selecionar Pasta de Saída");
        java.io.File selectedDirectory = directoryChooser.showDialog(listViewDocumentos.getScene().getWindow());
        if (selectedDirectory != null) {
            pastaSaidaAtual = selectedDirectory;
            txtPastaSaida.setText(selectedDirectory.getAbsolutePath());
            atualizarStatus();
        }
    }

    private void atualizarStatus() {
        lblStatusConexao.setText(mongoService.isConnected() ? "Conectado a MongoDB" : "MongoDB Desconectado");
        lblStatusConexao.setTextFill(
                mongoService.isConnected() ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);

        lblStatusPastaEntrada
                .setText("Entrada: " + (pastaEntradaAtual != null ? pastaEntradaAtual.getName() : "Nenhuma"));
        lblStatusPastaSaida.setText("Saída: " + (pastaSaidaAtual != null ? pastaSaidaAtual.getName() : "Nenhuma"));
    }

    public void setDocumento(DocumentoVeiculoDTO doc) {
        this.documentoAtual = doc;
        if (doc == null) {
            limparCampos();
            return;
        }

        preencherCampos();
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

        // 1. Salvar no MongoDB
        mongoService.salvarDocumento(documentoAtual);

        // 2. Exportar se necessário
        if (rbMongoCsv.isSelected()) {
            if (pastaSaidaAtual == null) {
                showAlert("Erro", "Selecione uma pasta de saída para exportar o CSV.");
                return;
            }
            try {
                com.automacao.auditoria.strategy.ExportStrategy strategy = new com.automacao.auditoria.strategy.CsvExportStrategy();
                strategy.exportar(documentoAtual, pastaSaidaAtual);
                showAlert("Sucesso", "Documento aprovado e exportado para CSV.");
            } catch (Exception e) {
                showAlert("Erro na Exportação", "Erro ao exportar CSV: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            showAlert("Sucesso", "Documento aprovado e salvo no MongoDB.");
        }

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

        documentoAtual.setPlaca(atualizarCampo(documentoAtual.getPlaca(), txtPlaca.getText()));
        documentoAtual.setChassi(atualizarCampo(documentoAtual.getChassi(), txtChassi.getText()));
        documentoAtual.setModelo(atualizarCampo(documentoAtual.getModelo(), txtModelo.getText()));
        documentoAtual.setMarca(atualizarCampo(documentoAtual.getMarca(), txtMarca.getText()));
        documentoAtual.setFabricacao(atualizarCampo(documentoAtual.getFabricacao(), txtAnoFab.getText()));
        documentoAtual.setRenavam(atualizarCampo(documentoAtual.getRenavam(), txtRenavam.getText()));
        documentoAtual.setCpfCnpj(atualizarCampo(documentoAtual.getCpfCnpj(), txtCpfCnpj.getText()));

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

    // Método auxiliar para criar ou atualizar CampoExtraido corretamente
    private CampoExtraido atualizarCampo(CampoExtraido campo, String novoValor) {
        if (campo == null) {
            // Usa o construtor correto de 3 parâmetros: (valor, status, motivo)
            return new CampoExtraido(novoValor, CampoStatus.OK, "Editado Manualmente");
        }
        campo.setValor(novoValor);
        return campo;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
