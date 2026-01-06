# Schema do Banco de Dados (MongoDB)

O sistema utiliza o MongoDB para armazenar os dados dos veículos processados e logs da Fipe.

## Collection: `veiculos_processados`

Armazena o documento final consolidado de cada veículo.

```json
{
  "_id": "ObjectId(...)",
  "placa": {
    "valor": "ABC1D23",
    "status": "OK",
    "confianca": 1.0,
    "origem": "REGEX"
  },
  "chassi": {
    "valor": "9BW...",
    "status": "OK"
  },
  "marca": { "valor": "FIAT/MOBI", "status": "OK" },
  "modelo": { "valor": "LIKE", "status": "OK" },
  "fabricacao": { "valor": "2021", "status": "OK" },
  "renavam": { "valor": "12345678900", "status": "OK" },
  "cpf_cnpj": { "valor": "000.000.000-00", "status": "OK" },
  "nome_proprietario": { "valor": "FULANO DE TAL", "status": "OK" },
  "status_extracao": "COMPLETO",
  "necessita_revisao": false,
  "data_processamento": "2023-10-27T10:00:00Z",
  "fipe_valor": 50000.00,
  "fipe_codigo": "001234-5",
  "fipe_info": {
    "price": 50000.00,
    "brand": "Fiat",
    "model": "Mobi Like...",
    "modelYear": 2021,
    "codeFipe": "001234-5"
  },
  "auditoria": [
    {
      "campo": "placa",
      "valor_anterior": "ABC1023",
      "valor_novo": "ABC1D23",
      "usuario": "LLM_FIX",
      "data_hora": "..."
    }
  ]
}
```

## Collection: `dados_fipe`

Armazena o histórico de consultas realizadas à API Fipe.

```json
{
  "_id": "ObjectId(...)",
  "placa_veiculo": "ABC1D23",
  "chassi_veiculo": "9BW...",
  "price": 50000.00,
  "brand": "Fiat",
  "model": "Mobi Like...",
  "modelYear": 2021,
  "codeFipe": "001234-5",
  "referenceMonth": "Outubro/2023",
  "data_consulta": "2023-10-27T10:00:00Z"
}
```
