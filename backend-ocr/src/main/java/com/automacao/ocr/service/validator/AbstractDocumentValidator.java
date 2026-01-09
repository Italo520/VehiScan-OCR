package com.automacao.ocr.service.validator;

import com.automacao.ocr.extraction.validators.ChassiValidator;
import com.automacao.ocr.extraction.validators.CpfCnpjValidator;
import com.automacao.ocr.extraction.validators.PlacaValidator;
import com.automacao.ocr.extraction.validators.RenavamValidator;
import com.automacao.ocr.model.CampoExtraido;
import com.automacao.ocr.model.CampoStatus;
import com.automacao.ocr.model.DocumentoVeiculoDTO;

import java.time.Year;

public abstract class AbstractDocumentValidator implements DocumentValidatorStrategy {

    protected static final int ANO_MIN = 1950;
    protected static final int ANO_MAX = Year.now().getValue() + 1;

    protected final PlacaValidator placaValidator;
    protected final ChassiValidator chassiValidator;
    protected final RenavamValidator renavamValidator;
    protected final CpfCnpjValidator cpfCnpjValidator;

    public AbstractDocumentValidator() {
        this.placaValidator = new PlacaValidator();
        this.chassiValidator = new ChassiValidator();
        this.renavamValidator = new RenavamValidator();
        this.cpfCnpjValidator = new CpfCnpjValidator();
    }

    @Override
    public void validar(DocumentoVeiculoDTO doc, String textoOriginal) {
        validarPlaca(doc);
        validarChassi(doc);
        validarRenavam(doc);
        validarCpfCnpj(doc);
        validarAnoFabricacao(doc);
        validarAnoModelo(doc);
        validarMarcaModelo(doc);
        validarEspecifico(doc, textoOriginal);
    }

    /**
     * Método gancho para validações específicas de cada tipo de documento.
     */
    protected abstract void validarEspecifico(DocumentoVeiculoDTO doc, String textoOriginal);

    protected void validarPlaca(DocumentoVeiculoDTO doc) {
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

    protected void validarChassi(DocumentoVeiculoDTO doc) {
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

    protected void validarRenavam(DocumentoVeiculoDTO doc) {
        CampoExtraido renavam = doc.getRenavam();
        if (renavam == null || renavam.getValor() == null || renavam.getValor().isBlank()) {
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

    protected void validarCpfCnpj(DocumentoVeiculoDTO doc) {
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

    protected void validarAnoFabricacao(DocumentoVeiculoDTO doc) {
        validarAno(doc.getFabricacao(), "fabricação");
    }

    protected void validarAnoModelo(DocumentoVeiculoDTO doc) {
        validarAno(doc.getModelo(), "modelo");
    }

    protected void validarAno(CampoExtraido campo, String label) {
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

    protected void validarMarcaModelo(DocumentoVeiculoDTO doc) {
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

    protected void marcar(CampoExtraido campo, CampoStatus status, String motivo) {
        if (campo == null)
            return;
        campo.setStatus(status);
        campo.setMotivo(motivo);
    }
}
