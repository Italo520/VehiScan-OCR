package com.automacao.ocr.db;

import com.automacao.ocr.dto.DocumentoVeiculoDTO;
import com.automacao.ocr.fipe.dto.FipeCompletoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBService {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ObjectMapper mapper;

    public MongoDBService() {
        // Carrega variáveis de ambiente
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .load();

        String connectionString = dotenv.get("MONGO_URI", "mongodb://localhost:27017");

        // Configura conexão com timeout
        com.mongodb.MongoClientSettings settings = com.mongodb.MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(connectionString))
                .applyToSocketSettings(
                        builder -> builder.connectTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS))
                .applyToClusterSettings(
                        builder -> builder.serverSelectionTimeout(5, java.util.concurrent.TimeUnit.SECONDS))
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase("vehiscan_db");
        this.mapper = new ObjectMapper();
    }

    public void salvarVeiculo(DocumentoVeiculoDTO doc, FipeCompletoDTO fipeData) {
        try {
            MongoCollection<Document> collection = database.getCollection("veiculos_processados");

            // Cria documento base com dados do OCR
            Document mongoDoc = new Document();
            mongoDoc.append("placa", doc.getPlaca() != null ? doc.getPlaca().getValor() : null);
            mongoDoc.append("chassi", doc.getChassi() != null ? doc.getChassi().getValor() : null);
            mongoDoc.append("marca", doc.getMarca() != null ? doc.getMarca().getValor() : null);
            mongoDoc.append("modelo", doc.getModelo() != null ? doc.getModelo().getValor() : null);
            mongoDoc.append("fabricacao", doc.getFabricacao() != null ? doc.getFabricacao().getValor() : null);
            mongoDoc.append("tipo_documento",
                    doc.getTipoDocumento() != null ? doc.getTipoDocumento().getValor() : null);
            mongoDoc.append("data_processamento", java.time.Instant.now().toString());

            // Adiciona dados da Fipe se houver
            if (fipeData != null) {
                String fipeJson = mapper.writeValueAsString(fipeData);
                Document fipeDoc = Document.parse(fipeJson);
                mongoDoc.append("fipe_info", fipeDoc);
            }

            collection.insertOne(mongoDoc);
            System.out.println("   [MongoDB] Veículo salvo com sucesso. ID: " + mongoDoc.get("_id"));

        } catch (Exception e) {
            System.err.println("   [MongoDB] Erro ao salvar veículo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
