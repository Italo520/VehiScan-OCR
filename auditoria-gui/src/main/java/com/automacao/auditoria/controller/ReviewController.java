package com.automacao.auditoria.controller;

import com.automacao.auditoria.component.DocumentoListCell;
import com.automacao.auditoria.service.MongoService;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.StatusExtracao;
import com.automacao.ocr.model.CampoStatus;
import com.automacao.ocr.model.AuditLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.stream.Collectors;
import com.automacao.ocr.extraction.ExtractionPipeline;
import com.automacao.ocr.ocr.TesseractService;
import com.automacao.ocr.ocr.TesseractServiceImpl;
import com.automacao.ocr.service.ExtratorLLM;
import com.automacao.ocr.service.ExtratorLLMSimulado;
import com.automacao.ocr.service.ValidadorDocumentoVeiculo;
import com.automacao.ocr.service.ValidadorDocumentoVeiculoImpl;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Paths;
import javafx.concurrent.Task;

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
    @FXML
    private TextArea txtObservacoes;

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
                dto.setCaminhoArquivo(arquivo.getAbsolutePath());
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
            dto.setCaminhoArquivo(file.getAbsolutePath());
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
        carregarImagemOuPdf(doc);
    }

    private void carregarImagemOuPdf(DocumentoVeiculoDTO doc) {
        if (doc.getCaminhoArquivo() == null) {
            imageView.setImage(null);
            return;
        }

        try {
            java.io.File file = new java.io.File(doc.getCaminhoArquivo());
            if (!file.exists()) {
                System.out.println("Arquivo não encontrado: " + doc.getCaminhoArquivo());
                imageView.setImage(null);
                return;
            }

            String lowerName = file.getName().toLowerCase();
            if (lowerName.endsWith(".pdf")) {
                // Renderizar PDF usando PDFBox
                try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(
                            document);
                    // Renderiza a primeira página em 72 DPI (escala 1)
                    java.awt.image.BufferedImage bufferedImage = renderer.renderImage(0);
                    imageView.setImage(javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null));
                }
            } else {
                // Carregar Imagem normal
                imageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImage(null);
        }
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

        if (documentoAtual.getObservacoes() != null) {
            txtObservacoes.setText(documentoAtual.getObservacoes().getValor());
        } else {
            txtObservacoes.clear();
        }

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

        // OCR Raw
        if (documentoAtual.getOcrRaw() != null) {
            txtOcrRaw.setText(documentoAtual.getOcrRaw());
        } else {
            txtOcrRaw.clear();
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
    private void zoomIn() {
        imageView.setScaleX(imageView.getScaleX() * 1.1);
        imageView.setScaleY(imageView.getScaleY() * 1.1);
    }

    @FXML
    private void zoomOut() {
        imageView.setScaleX(imageView.getScaleX() / 1.1);
        imageView.setScaleY(imageView.getScaleY() / 1.1);
    }

    @FXML
    private void girarImagem() {
        imageView.setRotate(imageView.getRotate() + 90);
    }

    @FXML
    private void onNecessitaRevisaoChanged() {
        if (documentoAtual != null) {
            boolean novoValor = chkNecessitaRevisao.isSelected();
            if (documentoAtual.isNecessitaRevisao() != novoValor) {
                documentoAtual.setNecessitaRevisao(novoValor);
                // Logar mudança de status
                AuditLog log = new AuditLog("Necessita Revisão",
                        String.valueOf(!novoValor), String.valueOf(novoValor), "Auditor");
                documentoAtual.getAuditoria().add(log);
            }
        }
    }

    @FXML
    private void voltar() {
        // Deprecated in new layout
    }

    @FXML
    private void analisarTodos() {
        if (masterData.isEmpty()) {
            showAlert("Aviso", "Nenhum documento na lista para analisar.");
            return;
        }

        final boolean exportarCsv = rbMongoCsv.isSelected();
        final String pastaSaida = txtPastaSaida.getText();

        if (exportarCsv && (pastaSaida == null || pastaSaida.isEmpty())) {
            showAlert("Erro", "Selecione uma pasta de saída para exportar o CSV.");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Inicializando serviços...");

                // Carrega variáveis de ambiente
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

                // Configuração Tesseract
                java.nio.file.Path tessdata = Paths
                        .get("C:\\Users\\leiloespb\\Desktop\\Projetos\\automacao\\backend-ocr\\tessdata");
                TesseractService extratorTexto = new TesseractServiceImpl(tessdata, "por");

                // Configuração LLM
                ExtratorLLM extratorLLM;
                String apiKey = dotenv.get("PERPLEXITY_API_KEY");
                if (apiKey == null) {
                    apiKey = System.getenv("PERPLEXITY_API_KEY");
                }

                if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("pplx-xxxx")) {
                    extratorLLM = new com.automacao.ocr.service.ExtratorLLMPerplexity(apiKey);
                } else {
                    extratorLLM = new ExtratorLLMSimulado();
                }

                ValidadorDocumentoVeiculo validador = new ValidadorDocumentoVeiculoImpl();

                com.automacao.ocr.service.CsvExportService csvService = null;
                if (exportarCsv) {
                    String arquivoCsvStr = Paths.get(pastaSaida, "veiculos_lote.csv").toString();
                    java.io.File fileCsv = new java.io.File(arquivoCsvStr);
                    if (fileCsv.exists()) {
                        fileCsv.delete(); // Começar limpo para não duplicar em re-execuções
                    }
                    csvService = new com.automacao.ocr.service.CsvExportService(arquivoCsvStr);
                    updateMessage("Exportação CSV habilitada: " + arquivoCsvStr);
                }

                ExtractionPipeline pipeline = new ExtractionPipeline(extratorTexto, extratorLLM, validador, csvService);

                int total = masterData.size();
                int count = 0;

                for (DocumentoVeiculoDTO doc : masterData) {
                    if (isCancelled())
                        break;

                    // Pula se já estiver completo, MASEXPORTA SE NECESSÁRIO
                    if (doc.getStatusExtracao() == StatusExtracao.COMPLETO) {
                        if (csvService != null) {
                            updateMessage("Exportando (Cache): " + doc.getPlaca().getValor());
                            csvService.salvarVeiculo(doc);
                        }
                        count++;
                        updateProgress(count, total);
                        continue;
                    }

                    updateMessage("Analisando: "
                            + (doc.getCaminhoArquivo() != null ? new java.io.File(doc.getCaminhoArquivo()).getName()
                                    : "Documento " + (count + 1)));

                    try {
                        if (doc.getCaminhoArquivo() != null) {
                            java.io.File arquivo = new java.io.File(doc.getCaminhoArquivo());
                            if (arquivo.exists()) {
                                // Processa o arquivo
                                DocumentoVeiculoDTO resultado = pipeline.processar(arquivo);

                                // Atualiza o DTO original com os resultados
                                // Precisamos fazer isso na thread da UI ou copiar os dados com cuidado
                                // Como DTO é POJO, podemos atualizar, mas a UI só vai refletir se for
                                // Observable ou se forçarmos refresh

                                // Copiando dados principais
                                doc.setPlaca(resultado.getPlaca());
                                doc.setChassi(resultado.getChassi());
                                doc.setModelo(resultado.getModelo());
                                doc.setMarca(resultado.getMarca());
                                doc.setFabricacao(resultado.getFabricacao());
                                doc.setRenavam(resultado.getRenavam());
                                doc.setCpfCnpj(resultado.getCpfCnpj());
                                doc.setDadosFipe(resultado.getDadosFipe());
                                doc.setStatusExtracao(resultado.getStatusExtracao());
                                doc.setNecessitaRevisao(resultado.isNecessitaRevisao());
                                doc.setAuditoria(resultado.getAuditoria()); // Logs
                                doc.setOcrRaw(resultado.getOcrRaw()); // OCR Raw

                                // Forçar atualização da UI para este item específico
                                javafx.application.Platform.runLater(() -> {
                                    listViewDocumentos.refresh();
                                    // Se for o documento atualmente selecionado, recarregar os campos
                                    if (documentoAtual == doc) {
                                        preencherCampos();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Logar erro no documento se possível
                    }

                    count++;
                    updateProgress(count, total);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            unbindAndSetText(lblStatusPastaEntrada, "Análise concluída!");
            listViewDocumentos.refresh();
            if (documentoAtual != null) {
                setDocumento(documentoAtual); // Recarrega visualização atual
            }
            showAlert("Sucesso", "Análise em lote concluída.");
        });

        task.setOnFailed(e -> {
            unbindAndSetText(lblStatusPastaEntrada, "Erro na análise.");
            Throwable error = task.getException();
            showAlert("Erro", "Falha na análise: " + error.getMessage());
            error.printStackTrace();
        });

        // Bind progress (opcional, se tivermos uma barra de progresso)
        // progressBar.progressProperty().bind(task.progressProperty());

        lblStatusPastaEntrada.textProperty().bind(task.messageProperty());

        new Thread(task).start();
    }

    private void unbindAndSetText(Label label, String text) {
        if (label.textProperty().isBound()) {
            label.textProperty().unbind();
        }
        label.setText(text);
    }

    private void atualizarDTO() {
        if (documentoAtual == null)
            return;

        documentoAtual.setPlaca(atualizarCampoLogado(documentoAtual.getPlaca(), txtPlaca.getText(), "Placa"));
        documentoAtual.setChassi(atualizarCampoLogado(documentoAtual.getChassi(), txtChassi.getText(), "Chassi"));
        documentoAtual.setModelo(atualizarCampoLogado(documentoAtual.getModelo(), txtModelo.getText(), "Modelo"));
        documentoAtual.setMarca(atualizarCampoLogado(documentoAtual.getMarca(), txtMarca.getText(), "Marca"));
        documentoAtual
                .setFabricacao(atualizarCampoLogado(documentoAtual.getFabricacao(), txtAnoFab.getText(), "Ano Fab"));
        documentoAtual.setRenavam(atualizarCampoLogado(documentoAtual.getRenavam(), txtRenavam.getText(), "Renavam"));
        documentoAtual.setCpfCnpj(atualizarCampoLogado(documentoAtual.getCpfCnpj(), txtCpfCnpj.getText(), "CPF/CNPJ"));
        documentoAtual.setObservacoes(
                atualizarCampoLogado(documentoAtual.getObservacoes(), txtObservacoes.getText(), "Observações"));

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

    // Método auxiliar para criar ou atualizar CampoExtraido com Log
    private CampoExtraido atualizarCampoLogado(CampoExtraido campo, String novoValor, String nomeCampo) {
        String valorAntigo = (campo != null && campo.getValor() != null) ? campo.getValor() : "";
        String valorNovo = (novoValor != null) ? novoValor : "";

        if (!valorAntigo.equals(valorNovo)) {
            // Criar Log
            AuditLog log = new AuditLog(nomeCampo, valorAntigo, valorNovo, "Auditor");
            documentoAtual.getAuditoria().add(log);

            // Atualizar Campo
            if (campo == null) {
                return new CampoExtraido(valorNovo, CampoStatus.OK, "Editado Manualmente");
            }
            campo.setValor(valorNovo);
            campo.setStatus(CampoStatus.OK); // Assume OK se editado manualmente
        }
        return campo;
    }

    @FXML
    private void exportarListaCsv() {
        if (masterData.isEmpty()) {
            showAlert("Aviso", "Nenhum documento na lista para exportar.");
            return;
        }

        java.io.File pastaDestino = pastaSaidaAtual;
        if (pastaDestino == null) {
            String pathStr = txtPastaSaida.getText();
            if (pathStr != null && !pathStr.isEmpty()) {
                pastaDestino = new java.io.File(pathStr);
            }
        }

        if (pastaDestino == null || !pastaDestino.exists()) {
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("Selecionar Pasta para Exportar CSV");
            pastaDestino = directoryChooser.showDialog(listViewDocumentos.getScene().getWindow());
            if (pastaDestino == null)
                return;
        }

        try {
            // Usa o mesmo nome de arquivo do lote ou um específico?
            // Vamos usar 'veiculos_exportados.csv' para diferenciar ou o mesmo?
            // O usuário pediu "exportar somente o arquivo csv", pode ser um snapshot.
            String arquivoCsvStr = java.nio.file.Paths.get(pastaDestino.getAbsolutePath(), "veiculos_exportados.csv")
                    .toString();

            // Delete se existir para exportar apenas a lista atual limpa
            java.io.File fileCsv = new java.io.File(arquivoCsvStr);
            if (fileCsv.exists()) {
                fileCsv.delete();
            }

            com.automacao.ocr.service.CsvExportService csvService = new com.automacao.ocr.service.CsvExportService(
                    arquivoCsvStr);

            int count = 0;
            for (DocumentoVeiculoDTO doc : masterData) {
                csvService.salvarVeiculo(doc);
                count++;
            }

            showAlert("Sucesso", "Exportação concluída.\n" + count + " veículos salvos em:\n" + arquivoCsvStr);
        } catch (Exception e) {
            showAlert("Erro", "Falha ao exportar CSV: " + e.getMessage());
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
