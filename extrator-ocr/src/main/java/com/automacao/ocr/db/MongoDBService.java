package com.automacao.ocr.db;

import com.automacao.ocr.dto.CampoExtraido;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;
import com.automacao.ocr.fipe.dto.FipeCompletoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

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
        this.mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    public void salvarVeiculo(DocumentoVeiculoDTO doc, FipeCompletoDTO fipeData) {
        try {
            MongoCollection<Document> collection = database.getCollection("veiculos_processados");
            MongoCollection<Document> fipeCollection = database.getCollection("dados_fipe");

            // Cria documento base com dados do OCR
            Document mongoDoc = new Document();

            // Mapeia campos complexos
            mongoDoc.append("placa", converterCampo(doc.getPlaca()));
            mongoDoc.append("chassi", converterCampo(doc.getChassi()));
            mongoDoc.append("marca", converterCampo(doc.getMarca()));
            mongoDoc.append("modelo", converterCampo(doc.getModelo()));
            mongoDoc.append("fabricacao", converterCampo(doc.getFabricacao()));
            mongoDoc.append("tipo_documento", converterCampo(doc.getTipoDocumento()));
            mongoDoc.append("renavam", converterCampo(doc.getRenavam()));
            mongoDoc.append("cpf_cnpj", converterCampo(doc.getCpfCnpj()));
            mongoDoc.append("nome_proprietario", converterCampo(doc.getNomeProprietario()));

            // Metadados gerais
            mongoDoc.append("status_extracao", doc.getStatusExtracao().toString());
            mongoDoc.append("necessita_revisao", doc.isNecessitaRevisao());
            mongoDoc.append("data_processamento", java.time.Instant.now().toString());

            // Auditoria
            if (doc.getAuditoria() != null && !doc.getAuditoria().isEmpty()) {
                List<Document> auditDocs = doc.getAuditoria().stream()
                        .map(log -> new Document()
                                .append("campo", log.getCampo())
                                .append("valor_anterior", log.getValorAnterior())
                                .append("valor_novo", log.getValorNovo())
                                .append("usuario", log.getUsuario())
                                .append("data_hora", log.getDataHora().toString()))
                        .collect(Collectors.toList());
                mongoDoc.append("auditoria", auditDocs);
            }

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
                String placaVal = doc.getPlaca() != null ? doc.getPlaca().getValor() : null;
                String chassiVal = doc.getChassi() != null ? doc.getChassi().getValor() : null;

                Document fipeLog = Document.parse(fipeJson);
                fipeLog.append("placa_veiculo", placaVal);
                fipeLog.append("chassi_veiculo", chassiVal);
                fipeLog.append("data_consulta", java.time.Instant.now().toString());
                fipeCollection.insertOne(fipeLog);
            }

            // Define o filtro para busca (prioridade: Placa -> Chassi)
            String placa = doc.getPlaca() != null ? doc.getPlaca().getValor() : null;
            String chassi = doc.getChassi() != null ? doc.getChassi().getValor() : null;

            org.bson.conversions.Bson filter = null;
            if (placa != null && !placa.trim().isEmpty()) {
                // Busca dentro do objeto aninhado 'placa.valor'
                filter = com.mongodb.client.model.Filters.eq("placa.valor", placa);
            } else if (chassi != null && !chassi.trim().isEmpty()) {
                // Busca dentro do objeto aninhado 'chassi.valor'
                filter = com.mongodb.client.model.Filters.eq("chassi.valor", chassi);
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

    private Document converterCampo(CampoExtraido campo) {
        if (campo == null)
            return null;
        return new Document()
                .append("valor", campo.getValor())
                .append("status", campo.getStatus().toString())
                .append("motivo", campo.getMotivo())
                .append("confianca", campo.getConfianca())
                .append("origem", campo.getOrigem())
                .append("validado_manualmente", campo.isValidadoManualmente());
    }
}
