package com.automacao.ocr.service;

import com.automacao.ocr.dto.CampoExtraido;
import com.automacao.ocr.dto.CampoStatus;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidadorDocumentoVeiculoImpl implements ValidadorDocumentoVeiculo {

    private static final Pattern REGEX_PLACA_MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");
    private static final int ANO_MIN = 1950;
    private static final int ANO_MAX = Year.now().getValue() + 1;

    private final Set<String> marcasConhecidas;

    public ValidadorDocumentoVeiculoImpl() {
        this.marcasConhecidas = new HashSet<>();
        // Exemplo de carga inicial (idealmente viria de um DB ou arquivo)
        marcasConhecidas.add("GM/CELTA");
        marcasConhecidas.add("JEEP/RENEGADE");
        marcasConhecidas.add("FIAT/STRADA");
        marcasConhecidas.add("VW/GOL");
        marcasConhecidas.add("HONDA/CIVIC");
        marcasConhecidas.add("TOYOTA/COROLLA");
    }

    @Override
    public DocumentoVeiculoDTO validar(DocumentoVeiculoDTO doc, String textoOriginal) {
        validarPlaca(doc);
        validarAnoFabricacao(doc);
        validarAnoModelo(doc);
        validarMarcaModelo(doc);
        return doc;
    }

    private void validarPlaca(DocumentoVeiculoDTO doc) {
        CampoExtraido placa = doc.getPlaca();
        if (placa == null || placa.getValor() == null || placa.getValor().isBlank()) {
            marcar(placa, CampoStatus.NAO_ENCONTRADO, "placa ausente");
            return;
        }
        String normalizada = placa.getValor().toUpperCase().replaceAll("[^A-Z0-9]", "");
        placa.setValor(normalizada);

        if (REGEX_PLACA_MERCOSUL.matcher(normalizada).matches()) {
            marcar(placa, CampoStatus.OK, null);
        } else {
            marcar(placa, CampoStatus.SUSPEITO, "formato de placa inválido: " + normalizada);
        }
    }

    private void validarAnoFabricacao(DocumentoVeiculoDTO doc) {
        validarAno(doc.getFabricacao(), "fabricação");
    }

    private void validarAnoModelo(DocumentoVeiculoDTO doc) {
        validarAno(doc.getModelo(), "modelo");
    }

    private void validarAno(CampoExtraido campo, String label) {
        if (campo == null || campo.getValor() == null) {
            marcar(campo, CampoStatus.NAO_ENCONTRADO, "ano " + label + " não encontrado");
            return;
        }
        try {
            int ano = Integer.parseInt(campo.getValor().replaceAll("\\D", ""));
            if (ano >= ANO_MIN && ano <= ANO_MAX) {
                campo.setValor(String.valueOf(ano));
                marcar(campo, CampoStatus.OK, null);
            } else {
                marcar(campo, CampoStatus.SUSPEITO, "ano " + label + " fora do intervalo plausível: " + ano);
            }
        } catch (NumberFormatException e) {
            marcar(campo, CampoStatus.INVALIDO, "ano " + label + " inválido: " + campo.getValor());
        }
    }

    private void validarMarcaModelo(DocumentoVeiculoDTO doc) {
        CampoExtraido marca = doc.getMarca();
        if (marca != null && marca.getValor() != null) {
            String m = marca.getValor().toUpperCase();
            // Validação simples: contém alguma das marcas conhecidas?
            boolean conhecida = marcasConhecidas.stream().anyMatch(m::contains);

            if (conhecida) {
                marcar(marca, CampoStatus.OK, null);
            } else {
                marcar(marca, CampoStatus.SUSPEITO, "marca não encontrada na base conhecida");
            }
        } else {
            marcar(marca, CampoStatus.NAO_ENCONTRADO, "marca não encontrada");
        }
    }

    private void marcar(CampoExtraido campo, CampoStatus status, String motivo) {
        if (campo == null)
            return;
        campo.setStatus(status);
        campo.setMotivo(motivo);
    }
}
