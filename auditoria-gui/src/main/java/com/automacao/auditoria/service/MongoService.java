package com.automacao.auditoria.service;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.StatusExtracao;
import com.automacao.ocr.model.CampoStatus;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MongoService {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private boolean connected = false;

    public MongoService() {
        try {
            // Load .env from parent directory or current
            Dotenv dotenv = Dotenv.configure()
                    .directory("../backend-ocr") // Try to find it in backend module
                    .ignoreIfMissing()
                    .load();

            String connectionString = dotenv.get("MONGO_URI", "mongodb://localhost:27017");

            // Debug info
            System.err.println("=== DEBUG MONGO CONNECTION ===");
            System.err.println("Java Version: " + System.getProperty("java.version"));
            System.err.println("Java Vendor: " + System.getProperty("java.vendor"));
            System.err.println("TLS Protocols (Before): " + System.getProperty("jdk.tls.client.protocols"));

            // Force TLS 1.2 to avoid handshake issues with some Atlas clusters/JDK versions
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2");

            com.mongodb.MongoClientSettings settings = com.mongodb.MongoClientSettings.builder()
                    .applyConnectionString(new com.mongodb.ConnectionString(connectionString))
                    .applyToSslSettings(builder -> builder.enabled(true).invalidHostNameAllowed(true)) // Explicitly
                                                                                                       // enable SSL and
                                                                                                       // allow invalid
                                                                                                       // hostnames for
                                                                                                       // testing
                    .applyToSocketSettings(builder -> builder.connectTimeout(10000, TimeUnit.MILLISECONDS)) // 10s
                                                                                                            // timeout
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(10000, TimeUnit.MILLISECONDS)) // 10s
                                                                                                                     // timeout
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase("vehiscan_db");
            this.collection = database.getCollection("veiculos_processados");

            // Test connection
            this.database.runCommand(new Document("ping", 1));
            this.connected = true;
            System.out.println("Conectado ao MongoDB com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao conectar ao MongoDB: " + e.getMessage());
            this.connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public List<DocumentoVeiculoDTO> buscarPendentes() {
        List<DocumentoVeiculoDTO> lista = new ArrayList<>();
        if (!connected)
            return lista;

        try {
            for (Document doc : collection.find(Filters.eq("necessita_revisao", true))) {
                lista.add(converterParaDTO(doc));
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar documentos: " + e.getMessage());
        }
        return lista;
    }

    public void salvarDocumento(DocumentoVeiculoDTO dto) {
        if (!connected)
            return;

        String placa = dto.getPlaca() != null ? dto.getPlaca().getValor() : null;
        String chassi = dto.getChassi() != null ? dto.getChassi().getValor() : null;

        org.bson.conversions.Bson filter = null;
        if (placa != null && !placa.isEmpty()) {
            filter = Filters.eq("placa.valor", placa);
        } else if (chassi != null && !chassi.isEmpty()) {
            filter = Filters.eq("chassi.valor", chassi);
        }

        if (filter != null) {
            Document doc = converterParaDocument(dto);
            collection.replaceOne(filter, doc);
        }
    }

    private DocumentoVeiculoDTO converterParaDTO(Document doc) {
        DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();

        dto.setPlaca(converterParaCampo(doc.get("placa", Document.class)));
        dto.setChassi(converterParaCampo(doc.get("chassi", Document.class)));
        dto.setMarca(converterParaCampo(doc.get("marca", Document.class)));
        dto.setModelo(converterParaCampo(doc.get("modelo", Document.class)));
        dto.setFabricacao(converterParaCampo(doc.get("fabricacao", Document.class)));
        dto.setTipoDocumento(converterParaCampo(doc.get("tipo_documento", Document.class)));
        dto.setRenavam(converterParaCampo(doc.get("renavam", Document.class)));
        dto.setCpfCnpj(converterParaCampo(doc.get("cpf_cnpj", Document.class)));
        dto.setNomeProprietario(converterParaCampo(doc.get("nome_proprietario", Document.class)));

        if (doc.getString("status_extracao") != null) {
            dto.setStatusExtracao(StatusExtracao.valueOf(doc.getString("status_extracao")));
        }

        Boolean necessitaRevisao = doc.getBoolean("necessita_revisao");
        dto.setNecessitaRevisao(necessitaRevisao != null ? necessitaRevisao : false);

        return dto;
    }

    private CampoExtraido converterParaCampo(Document doc) {
        if (doc == null)
            return new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Load");

        String valor = doc.getString("valor");
        String statusStr = doc.getString("status");
        CampoStatus status = statusStr != null ? CampoStatus.valueOf(statusStr) : CampoStatus.NAO_ENCONTRADO;
        String motivo = doc.getString("motivo");
        Double confianca = doc.getDouble("confianca");
        if (confianca == null)
            confianca = 0.0;

        CampoExtraido campo = new CampoExtraido(valor, status, motivo);
        campo.setConfianca(confianca);
        return campo;
    }

    private Document converterParaDocument(DocumentoVeiculoDTO dto) {
        Document doc = new Document();
        doc.append("placa", converterCampo(dto.getPlaca()));
        doc.append("chassi", converterCampo(dto.getChassi()));
        doc.append("marca", converterCampo(dto.getMarca()));
        doc.append("modelo", converterCampo(dto.getModelo()));
        doc.append("fabricacao", converterCampo(dto.getFabricacao()));
        doc.append("tipo_documento", converterCampo(dto.getTipoDocumento()));
        doc.append("renavam", converterCampo(dto.getRenavam()));
        doc.append("cpf_cnpj", converterCampo(dto.getCpfCnpj()));
        doc.append("nome_proprietario", converterCampo(dto.getNomeProprietario()));

        doc.append("status_extracao", dto.getStatusExtracao().toString());
        doc.append("necessita_revisao", dto.isNecessitaRevisao());

        return doc;
    }

    private Document converterCampo(CampoExtraido campo) {
        if (campo == null)
            return null;
        return new Document()
                .append("valor", campo.getValor())
                .append("status", campo.getStatus().toString())
                .append("motivo", campo.getMotivo())
                .append("confianca", campo.getConfianca());
    }
}
