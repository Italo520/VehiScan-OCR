package com.automacao.ocr.service;

import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.CampoStatus;
import com.automacao.ocr.model.DocumentoVeiculoDTO;
import com.automacao.ocr.model.VeiculoJSONDTO;
import com.automacao.ocr.llm.LLMClient;
import com.automacao.ocr.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExtratorLLMPerplexity implements ExtratorLLM {

    private final LLMClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExtratorLLMPerplexity(String apiKey) {
        // Perplexity: base oficial
        // Modelo ajustado para 'sonar' que é o padrão atual estável
        this.client = new LLMClient(apiKey, "https://api.perplexity.ai", "sonar");
    }

    @Override
    public DocumentoVeiculoDTO extrairCampos(String textoDocumento, DocumentoVeiculoDTO dadosPreliminares) {
        String sugestaoJson = "";
        if (dadosPreliminares != null) {
            try {
                // Serializa apenas os valores para o prompt
                VeiculoJSONDTO sugestao = new VeiculoJSONDTO();
                sugestao.placa = dadosPreliminares.getPlaca() != null ? dadosPreliminares.getPlaca().getValor() : null;
                sugestao.chassi = dadosPreliminares.getChassi() != null ? dadosPreliminares.getChassi().getValor()
                        : null;
                sugestao.marca = dadosPreliminares.getMarca() != null ? dadosPreliminares.getMarca().getValor() : null;
                sugestao.fabricacao = dadosPreliminares.getFabricacao() != null
                        ? dadosPreliminares.getFabricacao().getValor()
                        : null;
                sugestao.modelo = dadosPreliminares.getModelo() != null ? dadosPreliminares.getModelo().getValor()
                        : null;
                sugestao.tipo_documento = dadosPreliminares.getTipoDocumento() != null
                        ? dadosPreliminares.getTipoDocumento().getValor()
                        : null;

                sugestaoJson = "\n\nDADOS PRELIMINARES (REGEX) PARA CONFERÊNCIA:\n"
                        + mapper.writeValueAsString(sugestao);
            } catch (Exception e) {
                // Ignora erro na sugestão
            }
        }

        String systemPrompt = """
                Você é um assistente especializado em extrair dados estruturados de documentos de veículos.
                Sua tarefa é ler o texto do documento e usar os dados preliminares (se fornecidos) apenas como referência.

                IMPORTANTE: Se houver divergência entre os dados preliminares e o texto do documento, CONFIE NO TEXTO DO DOCUMENTO.
                Seu objetivo é corrigir erros do OCR/Regex e preencher campos faltantes.

                Responda APENAS com um JSON válido.

                Instruções Específicas:
                - Procure atentamente por notas de rodapé ou campos de observações.
                - Se encontrar menções a "Remarcado", "Sinistro", "Leilão", "Recuperado", inclua no campo "observacoes".
                - Se não houver observações relevantes, deixe o campo null ou string vazia.

                Campos esperados:
                - tipo_documento, placa, chassi, marca, fabricacao, modelo, classificacao, observacoes.
                """;

        String userPrompt = """
                Leia o documento abaixo e devolva o JSON pedido.

                Documento:
                \"\"\"%s\"\"\"
                %s
                """.formatted(textoDocumento, sugestaoJson);

        try {
            System.out.println("   [ExtratorLLM] Enviando requisição para Perplexity...");
            String rawResponse = client.chat(systemPrompt, userPrompt);

            String content = JsonUtils.extrairContent(rawResponse);
            VeiculoJSONDTO jsonDto = mapper.readValue(content, VeiculoJSONDTO.class);
            DocumentoVeiculoDTO resultadoLLM = converterParaDominio(jsonDto);

            // Preservar campos que o LLM não extrai ou que já temos
            if (dadosPreliminares != null) {
                resultadoLLM.setCaminhoArquivo(dadosPreliminares.getCaminhoArquivo());
                resultadoLLM.setOcrRaw(dadosPreliminares.getOcrRaw());

                // Preservar Renavam e CPF/CNPJ se o LLM não retornou (ou se confiamos mais no
                // Regex para estes)
                // O LLM atual não está sendo solicitado para extrair Renavam/CPF, então
                // copiamos do preliminar
                if (resultadoLLM.getRenavam().getStatus() == CampoStatus.NAO_ENCONTRADO) {
                    resultadoLLM.setRenavam(dadosPreliminares.getRenavam());
                }
                if (resultadoLLM.getCpfCnpj().getStatus() == CampoStatus.NAO_ENCONTRADO) {
                    resultadoLLM.setCpfCnpj(dadosPreliminares.getCpfCnpj());
                }
                if (resultadoLLM.getNomeProprietario().getStatus() == CampoStatus.NAO_ENCONTRADO) {
                    resultadoLLM.setNomeProprietario(dadosPreliminares.getNomeProprietario());
                }
            }

            // Garantir que o OCR Raw esteja setado se não veio do preliminar
            if (resultadoLLM.getOcrRaw() == null) {
                resultadoLLM.setOcrRaw(textoDocumento);
            }

            return resultadoLLM;

        } catch (Exception e) {
            System.err.println("   [ExtratorLLM] Erro na chamada LLM: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   [ExtratorLLM] Detalhes: " + e.getCause().getMessage());
            }

            System.out.println("   [ExtratorLLM] >> Ativando FALLBACK para Regex (Simulado)...");
            // No fallback, retornamos a preliminar se existir, ou rodamos o regex do zero
            if (dadosPreliminares != null)
                return dadosPreliminares;
            return new ExtratorLLMSimulado().extrairCampos(textoDocumento, null);
        }
    }

    private DocumentoVeiculoDTO converterParaDominio(VeiculoJSONDTO json) {
        DocumentoVeiculoDTO doc = new DocumentoVeiculoDTO();

        doc.setTipoDocumento(criarCampo(json.tipo_documento));
        doc.setPlaca(criarCampo(json.placa));
        doc.setChassi(criarCampo(json.chassi));
        doc.setMarca(criarCampo(json.marca));
        doc.setFabricacao(criarCampo(json.fabricacao));
        doc.setModelo(criarCampo(json.modelo));
        doc.setClassificacao(criarCampo(json.classificacao));
        doc.setObservacoes(criarCampo(json.observacoes));

        return doc;
    }

    private CampoExtraido criarCampo(String valor) {
        if (valor == null || valor.equalsIgnoreCase("null")) {
            return new CampoExtraido(null, CampoStatus.NAO_ENCONTRADO, "Não retornado pelo LLM");
        }
        return new CampoExtraido(valor, CampoStatus.OK, "Extraído via LLM");
    }
}
