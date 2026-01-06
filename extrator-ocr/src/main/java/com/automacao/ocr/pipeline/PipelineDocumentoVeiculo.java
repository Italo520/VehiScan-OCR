package com.automacao.ocr.pipeline;

import com.automacao.ocr.ExtratorTexto;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;
import com.automacao.ocr.service.ExtratorLLM;
import com.automacao.ocr.service.ValidadorDocumentoVeiculo;

import java.io.File;
import java.io.IOException;

public class PipelineDocumentoVeiculo {

        private final ExtratorTexto extratorTexto;
        private final ExtratorLLM extratorLLM;
        private final ValidadorDocumentoVeiculo validador;

        public PipelineDocumentoVeiculo(ExtratorTexto extratorTexto,
                        ExtratorLLM extratorLLM,
                        ValidadorDocumentoVeiculo validador) {
                this.extratorTexto = extratorTexto;
                this.extratorLLM = extratorLLM;
                this.validador = validador;
        }

        public DocumentoVeiculoDTO processar(File arquivo) throws IOException {
                // 1. OCR Puro
                System.out.println("   [Pipeline] 1. Executando OCR...");
                var resultadoTexto = extratorTexto.extrairTexto(arquivo);
                String texto = resultadoTexto.getTextoCompleto();

                if (texto == null || texto.isBlank()) {
                        System.out.println("   [Pipeline] AVISO: OCR não retornou texto.");
                        return new DocumentoVeiculoDTO(); // Retorna vazio
                }

                // 2. Extração Preliminar (Regex)
                System.out.println("   [Pipeline] 2a. Extração Preliminar (Regex)...");
                // Precisamos instanciar o extrator de regex aqui ou injetá-lo.
                // Como ele não implementa ExtratorLLM, usamos direto.
                com.automacao.ocr.ExtratorDadosVeiculo extratorRegex = new com.automacao.ocr.ExtratorDadosVeiculo();
                var dadosMap = extratorRegex.extrairDados(texto);

                // Converte Map -> DTO para passar como sugestão
                DocumentoVeiculoDTO dadosPreliminares = converterMapParaDTO(dadosMap);

                // 3. Extração Final (LLM com revisão)
                System.out.println("   [Pipeline] 2b. Extração Final (LLM revisando Regex)...");
                DocumentoVeiculoDTO extraido = extratorLLM.extrairCampos(texto, dadosPreliminares);

                // 4. Validação e Regras de Negócio
                System.out.println("   [Pipeline] 3. Validando dados...");
                DocumentoVeiculoDTO validado = validador.validar(extraido, texto);

                // 5. Enriquecimento com Fipe
                com.automacao.ocr.fipe.dto.FipeCompletoDTO fipeCompleto = null;

                if (validado.getMarca().getStatus() == com.automacao.ocr.dto.CampoStatus.OK &&
                                validado.getModelo().getStatus() == com.automacao.ocr.dto.CampoStatus.OK) {

                        System.out.println("   [Pipeline] 4. Consultando Tabela Fipe...");
                        com.automacao.ocr.fipe.FipeService fipeService = new com.automacao.ocr.fipe.FipeService();

                        // Extrai marca e modelo (que podem vir juntos no campo marca)
                        String marcaBruta = validado.getMarca().getValor();
                        String marca = marcaBruta;
                        String modelo = marcaBruta; // Inicialmente assume que pode ser tudo junto

                        // Limpeza de lixo comum de OCR
                        if (marca.contains("ASSINADO DIGITALMENTE")) {
                                marca = marca.split("ASSINADO DIGITALMENTE")[0].trim();
                        }

                        // Tenta separar Marca/Modelo
                        if (marca.contains("/")) {
                                String[] partes = marca.split("/");
                                if (partes.length > 1) {
                                        String p0 = partes[0].trim();
                                        String p1 = partes[1].trim();

                                        if (!p0.isEmpty()) {
                                                marca = p0;
                                                modelo = p1;
                                        } else if (partes.length > 2) {
                                                // Caso: "/ FIAT / STRADA" -> p0="", p1="FIAT", p2="STRADA"
                                                marca = p1;
                                                modelo = partes[2].trim();
                                        } else {
                                                // Caso: "/ FIAT" -> assume que é a marca
                                                marca = p1;
                                                modelo = p1;
                                        }
                                }
                        }

                        // Preferencialmente usa o Ano Modelo
                        String anoModelo = validado.getModelo().getValor();
                        String anoFabricacao = validado.getFabricacao() != null ? validado.getFabricacao().getValor()
                                        : null;

                        // Tenta primeiro com o Ano Modelo (preferencial)
                        var valorFipe = fipeService.buscarVeiculo(marca, modelo, anoModelo);

                        // Se falhar e tiver ano de fabricação diferente, tenta com ele
                        if (valorFipe == null && anoFabricacao != null && !anoFabricacao.equals(anoModelo)) {
                                System.out.println("   [Pipeline] >> Tentando busca com ano de fabricação: "
                                                + anoFabricacao);
                                valorFipe = fipeService.buscarVeiculo(marca, modelo, anoFabricacao);
                        }

                        if (valorFipe != null) {
                                validado.setDadosFipe(valorFipe);
                                // Converte para o DTO completo solicitado pelo usuário
                                fipeCompleto = new com.automacao.ocr.fipe.dto.FipeCompletoDTO(valorFipe);
                                System.out.println("   [Pipeline] >> Fipe Encontrada: " + valorFipe.valor);
                        } else {
                                System.out.println("   [Pipeline] >> Fipe não encontrada para: " + marca + " - "
                                                + modelo + " (Ano Modelo: " + anoModelo + ", Ano Fab: " + anoFabricacao
                                                + ")");
                        }
                }

                // 6. Persistência (MongoDB)
                System.out.println("   [Pipeline] 5. Salvando no MongoDB...");
                com.automacao.ocr.db.MongoDBService mongoService = new com.automacao.ocr.db.MongoDBService();
                mongoService.salvarVeiculo(validado, fipeCompleto);

                return validado;
        }

        private DocumentoVeiculoDTO converterMapParaDTO(java.util.Map<String, String> map) {
                DocumentoVeiculoDTO dto = new DocumentoVeiculoDTO();
                dto.setPlaca(new com.automacao.ocr.dto.CampoExtraido(map.get("Placa"),
                                com.automacao.ocr.dto.CampoStatus.OK,
                                "Regex"));
                dto.setChassi(new com.automacao.ocr.dto.CampoExtraido(map.get("Chassi"),
                                com.automacao.ocr.dto.CampoStatus.OK,
                                "Regex"));
                dto.setFabricacao(new com.automacao.ocr.dto.CampoExtraido(map.get("Fabricação"),
                                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
                dto.setModelo(new com.automacao.ocr.dto.CampoExtraido(map.get("Ano Modelo"),
                                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
                dto.setMarca(new com.automacao.ocr.dto.CampoExtraido(map.get("Marca/Modelo"),
                                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
                dto.setTipoDocumento(new com.automacao.ocr.dto.CampoExtraido(map.get("Tipo Documento"),
                                com.automacao.ocr.dto.CampoStatus.OK, "Regex"));
                return dto;
        }
}
