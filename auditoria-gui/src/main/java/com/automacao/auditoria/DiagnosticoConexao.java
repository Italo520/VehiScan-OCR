package com.automacao.auditoria;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

public class DiagnosticoConexao {

    public static void main(String[] args) {
        System.out.println("=== Diagnóstico de Conexão MongoDB ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Arch: " + System.getProperty("os.arch"));

        String uri = "mongodb+srv://VehiScan:VehiScanmongo@vehiscan.jp69e0s.mongodb.net/";
        System.out.println("URI: " + uri);

        // Tenta forçar TLS 1.2
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.out.println("TLS Protocols set to: " + System.getProperty("jdk.tls.client.protocols"));

        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSslSettings(builder -> {
                        builder.enabled(true);
                        builder.invalidHostNameAllowed(true); // Teste permissivo
                    })
                    .applyToSocketSettings(builder -> builder.connectTimeout(10000, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(10000, TimeUnit.MILLISECONDS))
                    .build();

            System.out.println("Tentando conectar...");
            try (MongoClient client = MongoClients.create(settings)) {
                MongoDatabase db = client.getDatabase("vehiscan_db");
                System.out.println("Database obtido. Executando ping...");
                db.runCommand(new Document("ping", 1));
                System.out.println("SUCESSO: Ping realizado com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("FALHA: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
