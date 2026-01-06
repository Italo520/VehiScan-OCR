package com.automacao.auditoria.controller;

import com.automacao.auditoria.service.MongoService;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;

public class DocumentListController {

    @FXML
    private TableView<DocumentoVeiculoDTO> tabelaDocumentos;

    @FXML
    private TableColumn<DocumentoVeiculoDTO, String> colPlaca;

    @FXML
    private TableColumn<DocumentoVeiculoDTO, String> colChassi;

    @FXML
    private TableColumn<DocumentoVeiculoDTO, String> colModelo;

    @FXML
    private TableColumn<DocumentoVeiculoDTO, String> colStatus;

    private MongoService mongoService;

    public void initialize() {
        mongoService = new MongoService();

        colPlaca.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getPlaca() != null ? cellData.getValue().getPlaca().getValor() : ""));

        colChassi.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getChassi() != null ? cellData.getValue().getChassi().getValor() : ""));

        colModelo.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getModelo() != null ? cellData.getValue().getModelo().getValor() : ""));

        colStatus.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getStatusExtracao().toString()));

        carregarDados();
    }

    private void carregarDados() {
        if (!mongoService.isConnected()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erro de Conexão");
            alert.setHeaderText("Não foi possível conectar ao MongoDB");
            alert.setContentText(
                    "Verifique se o banco de dados está rodando. A aplicação funcionará em modo offline (somente leitura/interface).");
            alert.showAndWait();
            return;
        }

        ObservableList<DocumentoVeiculoDTO> dados = FXCollections.observableArrayList(mongoService.buscarPendentes());
        tabelaDocumentos.setItems(dados);

        tabelaDocumentos.setRowFactory(tv -> {
            javafx.scene.control.TableRow<DocumentoVeiculoDTO> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    DocumentoVeiculoDTO rowData = row.getItem();
                    abrirRevisao(rowData);
                }
            });
            return row;
        });
    }

    private void abrirRevisao(DocumentoVeiculoDTO doc) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/review_view.fxml"));
            javafx.scene.Parent root = loader.load();

            ReviewController controller = loader.getController();
            controller.setDocumento(doc);

            tabelaDocumentos.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
