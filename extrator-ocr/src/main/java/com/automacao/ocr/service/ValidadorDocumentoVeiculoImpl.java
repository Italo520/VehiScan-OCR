package com.automacao.ocr.service;

import com.automacao.ocr.dto.CampoExtraido;
import com.automacao.ocr.dto.CampoStatus;
import com.automacao.ocr.dto.DocumentoVeiculoDTO;
import com.automacao.ocr.pipeline.validators.ChassiValidator;
import com.automacao.ocr.pipeline.validators.CpfCnpjValidator;
import com.automacao.ocr.pipeline.validators.PlacaValidator;
import com.automacao.ocr.pipeline.validators.RenavamValidator;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;

public class ValidadorDocumentoVeiculoImpl implements ValidadorDocumentoVeiculo {

    private static final int ANO_MIN = 1950;
    private static final int ANO_MAX = Year.now().getValue() + 1;

    private final Set<String> marcasConhecidas;
    private final PlacaValidator placaValidator;
    private final ChassiValidator chassiValidator;
    private final RenavamValidator renavamValidator;
    private final CpfCnpjValidator cpfCnpjValidator;

    public ValidadorDocumentoVeiculoImpl() {
        this.marcasConhecidas = new HashSet<>();
        // Exemplo de carga inicial
        marcasConhecidas.add("GM/CELTA");
        marcasConhecidas.add("JEEP/RENEGADE");
        marcasConhecidas.add("FIAT/STRADA");
        marcasConhecidas.add("VW/GOL");
        marcasConhecidas.add("HONDA/CIVIC");
        marcasConhecidas.add("TOYOTA/COROLLA");

        this.placaValidator = new PlacaValidator();
        this.chassiValidator = new ChassiValidator();
        this.renavamValidator = new RenavamValidator();
        this.cpfCnpjValidator = new CpfCnpjValidator();
    }

    @Override
    public DocumentoVeiculoDTO validar(DocumentoVeiculoDTO doc, String textoOriginal) {
        validarPlaca(doc);
        validarChassi(doc);
        validarRenavam(doc);
        validarCpfCnpj(doc);
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

        String valor = placa.getValor();
        if (placaValidator.isValid(valor)) {
            placa.setValor(placaValidator.refine(valor));
            marcar(placa, CampoStatus.OK, null);
        } else {
            marcar(placa, CampoStatus.SUSPEITO, "formato de placa inválido: " + valor);
        }
    }

    private void validarChassi(DocumentoVeiculoDTO doc) {
        CampoExtraido chassi = doc.getChassi();
        if (chassi == null || chassi.getValor() == null || chassi.getValor().isBlank()) {
            marcar(chassi, CampoStatus.NAO_ENCONTRADO, "chassi ausente");
            return;
        }

        String valor = chassi.getValor();
        if (chassiValidator.isValid(valor)) {
            chassi.setValor(chassiValidator.refine(valor));
            marcar(chassi, CampoStatus.OK, null);
        } else {
            marcar(chassi, CampoStatus.SUSPEITO, "formato de chassi inválido: " + valor);
        }
    }

    private void validarRenavam(DocumentoVeiculoDTO doc) {
        CampoExtraido renavam = doc.getRenavam();
        if (renavam == null || renavam.getValor() == null || renavam.getValor().isBlank()) {
            // Renavam pode ser opcional dependendo do documento, mas se veio, valida.
            // Se não veio, marca como não encontrado.
            marcar(renavam, CampoStatus.NAO_ENCONTRADO, "renavam ausente");
            return;
        }

        String valor = renavam.getValor();
        if (renavamValidator.isValid(valor)) {
            renavam.setValor(renavamValidator.refine(valor));
            marcar(renavam, CampoStatus.OK, null);
        } else {
            marcar(renavam, CampoStatus.SUSPEITO, "renavam inválido: " + valor);
        }
    }

    private void validarCpfCnpj(DocumentoVeiculoDTO doc) {
        CampoExtraido cpfCnpj = doc.getCpfCnpj();
        if (cpfCnpj == null || cpfCnpj.getValor() == null || cpfCnpj.getValor().isBlank()) {
            marcar(cpfCnpj, CampoStatus.NAO_ENCONTRADO, "cpf/cnpj ausente");
            return;
        }

        String valor = cpfCnpj.getValor();
        if (cpfCnpjValidator.isValid(valor)) {
            cpfCnpj.setValor(cpfCnpjValidator.refine(valor));
            marcar(cpfCnpj, CampoStatus.OK, null);
        } else {
            marcar(cpfCnpj, CampoStatus.SUSPEITO, "cpf/cnpj inválido: " + valor);
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
            if (!m.trim().isEmpty()) {
                marcar(marca, CampoStatus.OK, null);
            } else {
                marcar(marca, CampoStatus.NAO_ENCONTRADO, "marca vazia");
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
