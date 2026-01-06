package com.automacao.ocr.fipe;

import com.automacao.ocr.fipe.dto.ReferenciaFipeDTO;
import com.automacao.ocr.fipe.dto.ValorFipeDTO;

import java.util.List;
import java.util.stream.Collectors;

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

            // 2. Buscar lista de modelos da marca
            List<ReferenciaFipeDTO> modelos = client.listarModelos(codigoMarca);
            if (modelos == null || modelos.isEmpty()) {
                System.out.println("   [Fipe] Nenhum modelo encontrado para a marca ID: " + codigoMarca);
                return null;
            }

            String termoModelo = nomeModelo.toLowerCase();

            // 3. Filtrar e ordenar candidatos (Top 10)
            List<ReferenciaFipeDTO> candidatos = modelos.stream()
                    .filter(m -> calcularScore(m.nome.toLowerCase(), termoModelo) > 0)
                    .sorted((m1, m2) -> Double.compare(
                            calcularScore(m2.nome.toLowerCase(), termoModelo),
                            calcularScore(m1.nome.toLowerCase(), termoModelo)))
                    .limit(10)
                    .collect(Collectors.toList());

            if (candidatos.isEmpty()) {
                System.out.println("   [Fipe] Nenhum modelo candidato identificado para: " + nomeModelo);
                return null;
            }

            System.out.println("   [Fipe] Candidatos identificados para '" + nomeModelo + "': "
                    + candidatos.stream().map(m -> m.nome).collect(Collectors.joining(", ")));

            // 4. Tentar encontrar o ano em cada candidato (do melhor score para o pior)
            for (ReferenciaFipeDTO modelo : candidatos) {
                String codigoAno = encontrarCodigoAno(codigoMarca, modelo.codigo, anoModelo);

                if (codigoAno != null) {
                    System.out.println("   [Fipe] Match encontrado! Modelo: " + modelo.nome + " | Ano: " + codigoAno);
                    return client.consultarValor(codigoMarca, modelo.codigo, codigoAno);
                }
            }

            System.out.println("   [Fipe] Ano " + anoModelo + " não encontrado em nenhum dos " + candidatos.size()
                    + " melhores candidatos.");
            return null;

        } catch (Exception e) {
            System.err.println("   [Fipe] Erro no fluxo de busca: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String encontrarCodigoMarca(String busca) {
        List<ReferenciaFipeDTO> marcas = client.listarMarcas();
        String termo = normalizarMarca(busca);

        return marcas.stream()
                .filter(m -> m.nome.toLowerCase().contains(termo) || termo.contains(m.nome.toLowerCase()))
                .findFirst()
                .map(m -> m.codigo)
                .orElse(null);
    }

    private String encontrarCodigoAno(String codigoMarca, String codigoModelo, String anoBusca) {
        try {
            List<ReferenciaFipeDTO> anos = client.listarAnos(codigoMarca, codigoModelo);

            // 1. Filtra anos que começam com o ano buscado
            List<ReferenciaFipeDTO> candidatos = anos.stream()
                    .filter(a -> a.nome.startsWith(anoBusca))
                    .collect(Collectors.toList());

            if (candidatos.isEmpty()) {
                return null;
            }

            // 2. Prioriza Gasolina/Flex se houver múltiplos
            if (candidatos.size() > 1) {
                return candidatos.stream()
                        .filter(a -> a.nome.toLowerCase().contains("gasolina") || a.nome.toLowerCase().contains("flex"))
                        .findFirst()
                        .map(a -> a.codigo)
                        .orElse(candidatos.get(0).codigo);
            }

            return candidatos.get(0).codigo;
        } catch (Exception e) {
            return null;
        }
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
        if (m.contains("jeep"))
            return "jeep";

        String[] partes = m.split("/");
        for (String p : partes) {
            if (!p.trim().isEmpty()) {
                return p.trim();
            }
        }
        return m.trim();
    }

    private double calcularScore(String nomeFipe, String nomeBusca) {
        String[] tokensFipe = nomeFipe.split("\\s+");
        String[] tokensBusca = nomeBusca.split("[\\s/]+");

        int matches = 0;
        for (String tb : tokensBusca) {
            if (tb.length() <= 2)
                continue;
            for (String tf : tokensFipe) {
                // Verifica contensão mútua para lidar com typos parciais ou substrings
                if (tf.contains(tb) || tb.contains(tf)) {
                    matches++;
                    break; // Conta apenas uma vez por token de busca
                }
            }
        }
        // Penaliza nomes Fipe muito longos se tiverem poucos matches
        return (double) matches / tokensFipe.length;
    }
}
