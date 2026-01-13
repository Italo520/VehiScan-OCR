package com.automacao.ocr.app;

import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.extraction.ExtractionPipeline;
import com.automacao.ocr.ocr.TesseractService;
import com.automacao.ocr.ocr.TesseractServiceImpl;
import com.automacao.ocr.service.ExtratorLLM;
import com.automacao.ocr.service.ExtratorLLMSimulado;
import com.automacao.ocr.service.ValidadorDocumentoVeiculo;
import com.automacao.ocr.service.ValidadorDocumentoVeiculoImpl;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainExtracao {

    public static void main(String[] args) {
        try {
            // Carrega variáveis de ambiente do .env
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Configuração
            Path tessdata = Paths.get("C:\\Users\\leiloespb\\Desktop\\Projetos\\automacao\\extrator-ocr\\tessdata");

            // Inicialização dos componentes
            TesseractService extratorTexto = new TesseractServiceImpl(tessdata, "por");

            ExtratorLLM extratorLLM;
            // Tenta pegar do .env, se não tiver, tenta do ambiente do sistema
            String apiKey = dotenv.get("PERPLEXITY_API_KEY");
            if (apiKey == null) {
                apiKey = System.getenv("PERPLEXITY_API_KEY");
            }

            if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("pplx-xxxx")) {
                System.out.println(">> Usando Extrator LLM Perplexity (API Key detectada)");
                extratorLLM = new com.automacao.ocr.service.ExtratorLLMPerplexity(apiKey);
            } else {
                System.out.println(
                        ">> Usando Extrator LLM Simulado (Regex) - Configure PERPLEXITY_API_KEY no .env para usar LLM real");
                extratorLLM = new ExtratorLLMSimulado();
            }

            ValidadorDocumentoVeiculo validador = new ValidadorDocumentoVeiculoImpl();
            com.automacao.ocr.service.CsvExportService csvService = new com.automacao.ocr.service.CsvExportService(
                    "C:\\Users\\leiloespb\\Desktop\\Projetos\\automacao\\veiculos_extraidos.csv");

            ExtractionPipeline pipeline = new ExtractionPipeline(extratorTexto, extratorLLM, validador, csvService);

            // Pasta contendo os documentos
            File pastaDocs = new File("C:\\Users\\leiloespb\\Desktop\\Projetos\\automacao\\docs");

            if (!pastaDocs.exists() || !pastaDocs.isDirectory()) {
                System.out.println("Pasta de documentos não encontrada: " + pastaDocs.getAbsolutePath());
                return;
            }

            // Filtra PDFs e Imagens (Todos os arquivos da pasta)
            File[] arquivos = pastaDocs.listFiles((dir, name) -> {
                String n = name.toLowerCase();
                return n.endsWith(".pdf") || n.endsWith(".jpg") || n.endsWith(".png");
            });

            if (arquivos == null || arquivos.length == 0) {
                System.out.println("Nenhum arquivo encontrado para processamento.");
                return;
            }

            System.out.println("Iniciando Pipeline Robusto em " + arquivos.length + " arquivos...");

            for (File arquivo : arquivos) {
                System.out.println("\n==================================================");
                System.out.println("Arquivo: " + arquivo.getName());
                System.out.println("==================================================");

                try {
                    DocumentoVeiculoDTO doc = pipeline.processar(arquivo);

                    exibirResultado(doc);

                } catch (Exception e) {
                    System.out.println(">> ERRO CRÍTICO no pipeline: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("\nProcessamento concluído.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exibirResultado(DocumentoVeiculoDTO doc) {
        System.out.println("\n--- Resultado da Extração e Validação ---");
        System.out.println("Tipo Doc: " + doc.getTipoDocumento());
        System.out.println("Placa   : " + doc.getPlaca());
        System.out.println("Chassi  : " + doc.getChassi());
        System.out.println("Marca   : " + doc.getMarca());
        System.out.println("Fabr.   : " + doc.getFabricacao());
        System.out.println("Modelo  : " + doc.getModelo());
        if (doc.getDadosFipe() != null) {
            System.out.println("FIPE    : " + doc.getDadosFipe().valor + " (" + doc.getDadosFipe().modelo + ")");
        } else {
            System.out.println("FIPE    : Não encontrada");
        }
        System.out.println("-----------------------------------------");

        if (doc.getPlaca().getStatus() != com.automacao.ocr.model.CampoStatus.OK) {
            System.out.println("!!! ATENÇÃO: Problema com a Placa: " + doc.getPlaca().getMotivo());
        }
        if (doc.getChassi().getStatus() != com.automacao.ocr.model.CampoStatus.OK) {
            System.out.println("!!! ATENÇÃO: Problema com o Chassi: " + doc.getChassi().getMotivo());
        }
    }
}
