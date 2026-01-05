package com.automacao.ocr.fipe;

import com.automacao.ocr.fipe.dto.ModelosResponseDTO;
import com.automacao.ocr.fipe.dto.ReferenciaFipeDTO;
import com.automacao.ocr.fipe.dto.ValorFipeDTO;

import java.util.List;
import java.util.Optional;

public class FipeService {

    private final FipeClient client;

    public FipeService() {
        this.client = new FipeClient();
    }

    public ValorFipeDTO buscarVeiculo(String nomeMarca, String nomeModelo, String anoModelo) {
        try {
            // 1. Encontrar Marca
            String codigoMarca = encontrarCodigoMarca(nomeMarca);
            if (codigoMarca == null) {
                System.out.println("   [Fipe] Marca não encontrada: " + nomeMarca);
                return null;
            }

            // 2. Encontrar Modelo
            String codigoModelo = encontrarCodigoModelo(codigoMarca, nomeModelo);
            if (codigoModelo == null) {
                System.out
                        .println("   [Fipe] Modelo não encontrado: " + nomeModelo + " (Marca ID: " + codigoMarca + ")");
                return null;
            }

            // 3. Encontrar Ano
            String codigoAno = encontrarCodigoAno(codigoMarca, codigoModelo, anoModelo);
            if (codigoAno == null) {
                System.out.println("   [Fipe] Ano não encontrado: " + anoModelo);
                return null;
            }

            // 4. Consultar Valor
            return client.consultarValor(codigoMarca, codigoModelo, codigoAno);

        } catch (Exception e) {
            System.err.println("   [Fipe] Erro no fluxo de busca: " + e.getMessage());
            return null;
        }
    }

    private String encontrarCodigoMarca(String busca) {
        List<ReferenciaFipeDTO> marcas = client.listarMarcas();
        // Busca simples: contém o nome (ignora case)
        // Ex: "GM/CELTA" -> busca "GM" ou "CHEVROLET"
        // Melhoria: Mapear nomes comuns (GM -> Chevrolet)
        String termo = normalizarMarca(busca);

        return marcas.stream()
                .filter(m -> m.nome.toLowerCase().contains(termo) || termo.contains(m.nome.toLowerCase()))
                .findFirst()
                .map(m -> m.codigo)
                .orElse(null);
    }

    private String encontrarCodigoModelo(String codigoMarca, String buscaModelo) {
        ModelosResponseDTO response = client.listarModelos(codigoMarca);
        String termo = buscaModelo.toLowerCase();

        // Tenta match exato ou parcial
        // Aqui seria ideal usar Levenshtein ou LLM para desambiguar
        // Ex: "CELTA 5 PORTAS" vs "Celta Life 1.0 8V 4p"

        // Estratégia simples: Token mais significativo
        // Se buscaModelo for "GM/CELTA 5 PORTAS", tentamos achar "CELTA"

        return response.modelos.stream()
                .filter(m -> calcularScore(m.nome.toLowerCase(), termo) > 0)
                .sorted((m1, m2) -> Double.compare(calcularScore(m2.nome.toLowerCase(), termo),
                        calcularScore(m1.nome.toLowerCase(), termo))) // Decrescente
                .findFirst()
                .map(m -> m.codigo)
                .orElse(null);
    }

    private String encontrarCodigoAno(String codigoMarca, String codigoModelo, String anoBusca) {
        List<ReferenciaFipeDTO> anos = client.listarAnos(codigoMarca, codigoModelo);
        // Ano na Fipe vem como "2010-1" (Gasolina) ou "2010-3" (Diesel) ou "2010"
        // O documento traz "2010"

        return anos.stream()
                .filter(a -> a.nome.startsWith(anoBusca)) // "2010 " ou "2010-1"
                .findFirst()
                .map(a -> a.codigo)
                .orElse(null);
    }

    private String normalizarMarca(String marca) {
        if (marca == null)
            return "";
        String m = marca.toLowerCase();
        if (m.contains("gm") || m.contains("chev"))
            return "chevrolet";
        if (m.contains("vw") || m.contains("volks"))
            return "vw - volkswagen";
        if (m.contains("fiat"))
            return "fiat";
        if (m.contains("ford"))
            return "ford";
        if (m.contains("toyota"))
            return "toyota";
        if (m.contains("honda"))
            return "honda";
        return m.split("/")[0].trim(); // Pega a primeira parte se for composta "FIAT/STRADA" -> "fiat"
    }

    // Score simples de similaridade (Jaccard de palavras)
    private double calcularScore(String nomeFipe, String nomeBusca) {
        String[] tokensFipe = nomeFipe.split("\\s+");
        String[] tokensBusca = nomeBusca.split("[\\s/]+");

        int matches = 0;
        for (String tb : tokensBusca) {
            if (tb.length() <= 2)
                continue; // ignora curtos
            for (String tf : tokensFipe) {
                if (tf.contains(tb) || tb.contains(tf)) {
                    matches++;
                    break;
                }
            }
        }
        return (double) matches / tokensFipe.length;
    }
}
