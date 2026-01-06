# Arquitetura do Sistema

O sistema VehiScan OCR segue uma arquitetura de pipeline para processamento de documentos.

## Fluxo de Processamento

1.  **Entrada**: Arquivos (PDF ou Imagem) em uma pasta monitorada.
2.  **OCR (Tesseract)**:
    *   Converte o arquivo em texto bruto.
    *   Suporta PDFs nativos (extração direta) e escaneados (OCR).
3.  **Extração Preliminar (Regex)**:
    *   Aplica expressões regulares para identificar padrões conhecidos (Placa, Chassi, Renavam, etc).
    *   Gera um DTO preliminar.
4.  **Validação Inicial**:
    *   Verifica a validade dos dados extraídos (checksum de Chassi, formato de Placa).
5.  **Refinamento (LLM)**:
    *   **Condicional**: Se os dados críticos (Placa, Chassi) não forem encontrados ou estiverem inválidos, o texto é enviado para um LLM (Perplexity/OpenAI).
    *   O LLM tenta corrigir erros de OCR e inferir campos baseados no contexto.
6.  **Enriquecimento (Fipe)**:
    *   Com Marca, Modelo e Ano extraídos, o sistema consulta a API da Tabela Fipe.
    *   Adiciona informações de valor e código Fipe ao documento.
7.  **Persistência (MongoDB)**:
    *   Salva o documento processado na coleção `veiculos_processados`.
    *   Utiliza `upsert` baseado em Placa ou Chassi para evitar duplicatas.
    *   Registra logs de auditoria de alterações.

## Componentes Principais

*   **ExtractionPipeline**: Orquestrador do fluxo.
*   **TesseractService**: Wrapper para o Tesseract OCR.
*   **ExtratorDadosVeiculo**: Implementação dos Regex.
*   **ExtratorLLM**: Interface para clientes de LLM.
*   **FipeService**: Cliente da API Fipe com lógica de busca fuzzy (aproximada).
*   **VeiculoRepository**: Camada de acesso ao banco de dados.
