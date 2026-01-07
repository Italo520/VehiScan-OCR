package com.automacao.auditoria.component;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class DocumentoListCell extends ListCell<DocumentoVeiculoDTO> {

    private HBox content;
    private Label lblPlaca;
    private Label lblModelo;
    private Label lblStatus;
    private Circle statusIndicator;

    public DocumentoListCell() {
        super();
        lblPlaca = new Label();
        lblPlaca.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        lblModelo = new Label();
        lblModelo.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        lblStatus = new Label();
        lblStatus.getStyleClass().add("status-badge");

        statusIndicator = new Circle(5);

        VBox vBox = new VBox(lblPlaca, lblModelo);
        vBox.setSpacing(2);

        HBox rightBox = new HBox(lblStatus);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        content = new HBox(statusIndicator, vBox, rightBox);
        content.setSpacing(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(5));

        // Ensure rightBox takes available space to push status to right
        HBox.setHgrow(vBox, javafx.scene.layout.Priority.ALWAYS);
    }

    @Override
    protected void updateItem(DocumentoVeiculoDTO item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            String placa = item.getPlaca() != null ? item.getPlaca().getValor() : "S/ Placa";
            String modelo = item.getModelo() != null ? item.getModelo().getValor() : "Modelo Desconhecido";

            lblPlaca.setText(placa);
            lblModelo.setText(modelo);

            if (item.isNecessitaRevisao()) {
                lblStatus.setText("Revisão");
                lblStatus.setStyle(
                        "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 2 5 2 5;");
                statusIndicator.setFill(Color.ORANGE);
            } else {
                lblStatus.setText("OK");
                lblStatus.setStyle(
                        "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 2 5 2 5;");
                statusIndicator.setFill(Color.GREEN);
            }

            setText(null);
            setGraphic(content);
        }
    }
}
