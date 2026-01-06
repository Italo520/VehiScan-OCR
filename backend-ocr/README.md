# VehiScan OCR - Backend Module

Este módulo contém toda a lógica de processamento de documentos, OCR e validação de dados veiculares.

## Estrutura de Pacotes

*   `com.automacao.ocr.app`: Ponto de entrada da aplicação (`MainExtracao`).
*   `com.automacao.ocr.config`: Configurações gerais.
*   `com.automacao.ocr.extraction`: Pipeline de extração, regex e validadores.
*   `com.automacao.ocr.fipe`: Integração com API da Tabela Fipe.
*   `com.automacao.ocr.llm`: Cliente para LLMs (Perplexity, etc).
*   `com.automacao.ocr.model`: DTOs e modelos de dados.
*   `com.automacao.ocr.ocr`: Serviços de OCR (Tesseract).
*   `com.automacao.ocr.repository`: Acesso a dados (MongoDB).
*   `com.automacao.ocr.service`: Serviços de negócio e orquestração.
*   `com.automacao.ocr.utils`: Utilitários.

## Funcionalidades

1.  **OCR**: Extração de texto de imagens e PDFs usando Tesseract.
2.  **Regex**: Extração preliminar de campos (Placa, Chassi, etc.) via expressões regulares.
3.  **LLM**: Refinamento e correção de dados usando LLMs (opcional).
4.  **Validação**: Validação de formatos (Placa Mercosul/Antiga, Chassi, Renavam, CPF/CNPJ).
5.  **Fipe**: Enriquecimento automático com dados da Tabela Fipe.
6.  **Persistência**: Salvamento dos dados processados no MongoDB.

## Como Executar

Certifique-se de ter o arquivo `.env` configurado.

```bash
mvn exec:java
```
