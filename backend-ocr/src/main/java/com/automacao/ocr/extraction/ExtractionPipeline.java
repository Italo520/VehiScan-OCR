package com.automacao.ocr.extraction;

import com.automacao.ocr.ocr.TesseractService;
import com.automacao.ocr.model.CampoStatus;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.StatusExtracao;
import com.automacao.ocr.service.ExtratorLLM;
import com.automacao.ocr.service.ValidadorDocumentoVeiculo;

import java.io.File;
import java.io.IOException;

public class ExtractionPipeline {

        private final TesseractService extratorTexto;
        private final ExtratorLLM extratorLLM;
        private final ValidadorDocumentoVeiculo validador;
        private final com.automacao.ocr.service.CsvExportService csvExportService;

        public ExtractionPipeline(TesseractService extratorTexto,
                        ExtratorLLM extratorLLM,
                        ValidadorDocumentoVeiculo validador,
                        com.automacao.ocr.service.CsvExportService csvExportService) {
                this.extratorTexto = extratorTexto;
                this.extratorLLM = extratorLLM;
                this.validador = validador;
                this.csvExportService = csvExportService;
        }

        public DocumentoVeiculoDTO processar(File arquivo) throws IOException {
                // 1. OCR Puro
                System.out.println("   [Pipeline] 1. Executando OCR...");
                var resultadoTexto = extratorTexto.extrairTexto(arquivo);
                String texto = resultadoTexto.getTextoCompleto();

                if (texto == null || texto.isBlank()) {
                        System.out.println("   [Pipeline] AVISO: OCR não retornou texto.");
                        DocumentoVeiculoDTO vazio = new DocumentoVeiculoDTO();
                        vazio.setStatusExtracao(StatusExtracao.ERRO);
                        vazio.setNecessitaRevisao(true);
                        return vazio;
                }

                // 2. Extração Preliminar (Regex)
                System.out.println("   [Pipeline] 2a. Extração Preliminar (Regex)...");
                com.automacao.ocr.extraction.ExtratorDadosVeiculo extratorRegex = new com.automacao.ocr.extraction.ExtratorDadosVeiculo();
                var dadosMap = extratorRegex.extrairDados(texto);

                // Converte Map -> DTO
                DocumentoVeiculoDTO dadosPreliminares = converterMapParaDTO(dadosMap);
                dadosPreliminares.setOcrRaw(texto);

                // 3. Validação Preliminar
                System.out.println("   [Pipeline] 2b. Validação Preliminar...");
                DocumentoVeiculoDTO validadoPreliminar = validador.validar(dadosPreliminares, texto);

                // 4. Decisão de LLM (Fallback)
                DocumentoVeiculoDTO documentoFinal = validadoPreliminar;
                if (precisaRefinoLLM(validadoPreliminar)) {
                        System.out.println("   [Pipeline] 3. Extração Final (LLM Refinando)...");
                        // Passa os dados já validados (e potencialmente limpos) para o LLM
                        documentoFinal = extratorLLM.extrairCampos(texto, validadoPreliminar);

                        // Re-valida após LLM
                        System.out.println("   [Pipeline] 3b. Re-validando após LLM...");
                        documentoFinal = validador.validar(documentoFinal, texto);
                } else {
                        System.out.println("   [Pipeline] 3. Dados Regex suficientes e válidos. Pulando LLM.");
                }

                // 5. Enriquecimento com Fipe
                com.automacao.ocr.fipe.dto.FipeCompletoDTO fipeCompleto = null;

                if (documentoFinal.getMarca().getStatus() == CampoStatus.OK &&
                                documentoFinal.getModelo().getStatus() == CampoStatus.OK) {

                        System.out.println("   [Pipeline] 4. Consultando Tabela Fipe...");
                        com.automacao.ocr.fipe.FipeService fipeService = new com.automacao.ocr.fipe.FipeService();

                        String marcaBruta = documentoFinal.getMarca().getValor();
                        String marca = marcaBruta;
                        String modelo = marcaBruta;

                        if (marca.contains("ASSINADO DIGITALMENTE")) {
                                marca = marca.split("ASSINADO DIGITALMENTE")[0].trim();
                        }

                        if (marca.contains("/")) {
                                String[] partes = marca.split("/");
                                if (partes.length > 1) {
                                        String p0 = partes[0].trim();
                                        String p1 = partes[1].trim();

                                        if (!p0.isEmpty()) {
                                                marca = p0;
                                                modelo = p1;
                                        } else if (partes.length > 2) {
                                                marca = p1;
                                                modelo = partes[2].trim();
                                        } else {
                                                marca = p1;
                                                modelo = p1;
                                        }
                                }
                        }

                        String anoModelo = documentoFinal.getModelo().getValor();
                        String anoFabricacao = documentoFinal.getFabricacao() != null
                                        ? documentoFinal.getFabricacao().getValor()
                                        : null;

                        var valorFipe = fipeService.buscarVeiculo(marca, modelo, anoModelo);

                        if (valorFipe == null && anoFabricacao != null && !anoFabricacao.equals(anoModelo)) {
                                System.out.println("   [Pipeline] >> Tentando busca com ano de fabricação: "
                                                + anoFabricacao);
                                valorFipe = fipeService.buscarVeiculo(marca, modelo, anoFabricacao);
                        }

                        if (valorFipe != null) {
                                documentoFinal.setDadosFipe(valorFipe);
                                fipeCompleto = new com.automacao.ocr.fipe.dto.FipeCompletoDTO(valorFipe);
                                System.out.println("   [Pipeline] >> Fipe Encontrada: " + valorFipe.valor);
                        } else {
                                System.out.println("   [Pipeline] >> Fipe não encontrada para: " + marca + " - "
                                                + modelo + " (Ano Modelo: " + anoModelo + ", Ano Fab: " + anoFabricacao
                                                + ")");
                        }
                }

                // 6. Atualiza Status Final
                atualizarStatusFinal(documentoFinal);

                // 7. Persistência (MongoDB)
                System.out.println("   [Pipeline] 5. Salvando no MongoDB...");
                com.automacao.ocr.repository.VeiculoRepository mongoService = new com.automacao.ocr.repository.VeiculoRepository();
                mongoService.salvarVeiculo(documentoFinal, fipeCompleto);

                // 8. Salvar CSV (Se serviço configurado)
                if (csvExportService != null) {
                        System.out.println("   [Pipeline] 6. Salvando no CSV...");
                        csvExportService.salvarVeiculo(documentoFinal);
                }

                return documentoFinal;
        }

        private void atualizarStatusFinal(DocumentoVeiculoDTO doc) {
                boolean todosOk = true;
                boolean algumErro = false;

                // Verifica campos críticos
                if (doc.getPlaca().getStatus() != CampoStatus.OK)
                        todosOk = false;
                if (doc.getChassi().getStatus() != CampoStatus.OK)
                        todosOk = false;
                if (doc.getRenavam().getStatus() != CampoStatus.OK)
                        todosOk = false;
                if (doc.getMarca().getStatus() != CampoStatus.OK)
                        todosOk = false;

                if (doc.getPlaca().getStatus() == CampoStatus.INVALIDO)
                        algumErro = true;
                if (doc.getChassi().getStatus() == CampoStatus.INVALIDO)
                        algumErro = true;

                if (todosOk) {
                        doc.setStatusExtracao(StatusExtracao.COMPLETO);
                        doc.setNecessitaRevisao(false);
                } else if (algumErro) {
                        doc.setStatusExtracao(StatusExtracao.ERRO);
                        doc.setNecessitaRevisao(true);
                } else {
                        doc.setStatusExtracao(StatusExtracao.PARCIAL);
                        doc.setNecessitaRevisao(true);
                }
        }

        private boolean precisaRefinoLLM(DocumentoVeiculoDTO doc) {
                if (doc.getPlaca().getStatus() != CampoStatus.OK)
                        return true;
                if (doc.getChassi().getStatus() != CampoStatus.OK)
                        return true;
                if (doc.getRenavam().getStatus() != CampoStatus.OK)
                        return true;
                if (doc.getMarca().getStatus() != CampoStatus.OK)
                        return true;
                return false;
        }

        private DocumentoVeiculoDTO converterMapParaDTO(java.util.Map<String, String> map) {
                DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();
                dto.setPlaca(new com.automacao.ocr.model.CampoExtraido(map.get("Placa"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setChassi(new com.automacao.ocr.model.CampoExtraido(map.get("Chassi"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setFabricacao(new com.automacao.ocr.model.CampoExtraido(map.get("Fabricação"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setModelo(new com.automacao.ocr.model.CampoExtraido(map.get("Ano Modelo"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setMarca(new com.automacao.ocr.model.CampoExtraido(map.get("Marca/Modelo"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setTipoDocumento(new com.automacao.ocr.model.CampoExtraido(map.get("Tipo Documento"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));

                // Novos campos
                dto.setRenavam(new com.automacao.ocr.model.CampoExtraido(map.get("Renavam"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setCpfCnpj(new com.automacao.ocr.model.CampoExtraido(map.get("CPF/CNPJ"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));
                dto.setNomeProprietario(new com.automacao.ocr.model.CampoExtraido(map.get("Nome"),
                                CampoStatus.OK, "Regex", 1.0, "REGEX"));

                return dto;
        }
}
