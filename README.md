# VehiScan OCR

Projeto de automação para extração de dados de documentos veiculares (PDFs e Imagens) utilizando OCR (Tesseract) e LLMs (Perplexity/OpenAI).

## Estrutura do Projeto

O projeto é organizado como um multi-módulo Maven:

*   **backend-ocr**: Módulo backend responsável pela lógica de extração, validação, integração com APIs (Fipe, LLM) e persistência (MongoDB).
*   **frontend-fx** (Planejado): Interface gráfica em JavaFX para operação local.

## Pré-requisitos

*   Java 21+
*   Maven 3.8+
*   Tesseract OCR instalado no sistema (ou configurado via `tessdata`)
*   MongoDB rodando (local ou remoto)

## Como Construir

Na raiz do projeto:

```bash
mvn clean install
```

## Configuração

Crie um arquivo `.env` na raiz do módulo `backend-ocr` (ou na raiz do projeto, dependendo de como for executar) com as seguintes variáveis:

```properties
MONGO_URI=mongodb://localhost:27017
PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxx
FIPE_SUBSCRIPTION_TOKEN=seu_token_fipe_se_necessario
```

## Execução

Para rodar o backend de extração:

```bash
cd backend-ocr
mvn exec:java
```
