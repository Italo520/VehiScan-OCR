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
            MongoCollection<Document> fipeCollection = database.getCollection("dados_fipe");

            // Cria documento base com dados do OCR
            Document mongoDoc = new Document();
            String placa = doc.getPlaca() != null ? doc.getPlaca().getValor() : null;
            String chassi = doc.getChassi() != null ? doc.getChassi().getValor() : null;

            mongoDoc.append("placa", placa);
            mongoDoc.append("chassi", chassi);
            mongoDoc.append("marca", doc.getMarca() != null ? doc.getMarca().getValor() : null);
            mongoDoc.append("modelo", doc.getModelo() != null ? doc.getModelo().getValor() : null);
            mongoDoc.append("fabricacao", doc.getFabricacao() != null ? doc.getFabricacao().getValor() : null);
            mongoDoc.append("tipo_documento",
                    doc.getTipoDocumento() != null ? doc.getTipoDocumento().getValor() : null);
            mongoDoc.append("data_processamento", java.time.Instant.now().toString());

            // Adiciona dados da Fipe se houver
            if (fipeData != null) {
                // Apenas valor e código no corpo do documento (para queries rápidas)
                mongoDoc.append("fipe_valor", fipeData.price);
                mongoDoc.append("fipe_codigo", fipeData.codeFipe);

                // Mantém o objeto completo aninhado
                String fipeJson = mapper.writeValueAsString(fipeData);
                Document fipeDoc = Document.parse(fipeJson);
                mongoDoc.append("fipe_info", fipeDoc);

                // Salva na collection separada de dados da Fipe
                Document fipeLog = Document.parse(fipeJson);
                fipeLog.append("placa_veiculo", placa);
                fipeLog.append("chassi_veiculo", chassi);
                fipeLog.append("data_consulta", java.time.Instant.now().toString());
                fipeCollection.insertOne(fipeLog);
            }

            // Define o filtro para busca (prioridade: Placa -> Chassi)
            org.bson.conversions.Bson filter = null;
            if (placa != null && !placa.trim().isEmpty()) {
                filter = com.mongodb.client.model.Filters.eq("placa", placa);
            } else if (chassi != null && !chassi.trim().isEmpty()) {
                filter = com.mongodb.client.model.Filters.eq("chassi", chassi);
            }

            if (filter != null) {
                com.mongodb.client.model.ReplaceOptions options = new com.mongodb.client.model.ReplaceOptions()
                        .upsert(true);
                com.mongodb.client.result.UpdateResult result = collection.replaceOne(filter, mongoDoc, options);

                if (result.getUpsertedId() != null) {
                    System.out.println("   [MongoDB] Novo veículo inserido. ID: " + result.getUpsertedId());
                } else {
                    System.out.println("   [MongoDB] Veículo atualizado com sucesso (Placa: " + placa + ", Chassi: "
                            + chassi + ").");
                }
            } else {
                // Se não tiver nem placa nem chassi, insere como novo (não tem como identificar
                // duplicidade)
                collection.insertOne(mongoDoc);
                System.out.println("   [MongoDB] Veículo salvo (sem identificador único). ID: " + mongoDoc.get("_id"));
            }

        } catch (Exception e) {
            System.err.println("   [MongoDB] Erro ao salvar veículo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
