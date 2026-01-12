package com.automacao.ocr.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TesseractServiceImpl implements TesseractService {

    private final ITesseract tesseract;

    public TesseractServiceImpl(Path tessdataPath, String language) {
        this.tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath.toString()); // pasta com .traineddata
        tesseract.setLanguage(language); // "por", "por+eng", etc.
    }

    @Override
    public ResultadoExtracaoTexto extrairTexto(File arquivo) throws IOException {
        String nome = arquivo.getName().toLowerCase();

        if (nome.endsWith(".pdf")) {
            return extrairDePdf(arquivo);
        } else {
            return extrairDeImagem(arquivo);
        }
    }

    private ResultadoExtracaoTexto extrairDePdf(File arquivo) throws IOException {
        try (PDDocument doc = PDDocument.load(arquivo)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(doc);

            // Verifica se extraiu texto suficiente E relevante para considerar PDF de texto
            if (texto != null && !texto.isBlank() && texto.trim().length() > 20 && contemPalavrasChave(texto)) {
                // PDF texto “normal”
                ResultadoExtracaoTexto resultado = new ResultadoExtracaoTexto();
                resultado.setTipoFonte(TipoDocumentoFonte.PDF_TEXTO);
                resultado.setTextoCompleto(texto);
                return resultado;
            }

            // Se não há texto (ou muito pouco), tratar como escaneado (OCR página a página)
            PDFRenderer renderer = new PDFRenderer(doc);
            Map<Integer, String> porPagina = new LinkedHashMap<>();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                // 300 DPI é um bom balanço entre qualidade e performance para OCR
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                String textoPagina = tesseract.doOCR(image);
                porPagina.put(i + 1, textoPagina);
                sb.append(textoPagina).append("\n\n");
            }

            ResultadoExtracaoTexto resultado = new ResultadoExtracaoTexto();
            resultado.setTipoFonte(TipoDocumentoFonte.PDF_ESCANEADO);
            resultado.setTextoCompleto(sb.toString());
            resultado.setTextoPorPagina(porPagina);
            return resultado;
        } catch (TesseractException e) {
            throw new IOException("Erro ao executar OCR no PDF", e);
        }
    }

    private ResultadoExtracaoTexto extrairDeImagem(File arquivo) throws IOException {
        try {
            BufferedImage image = ImageIO.read(arquivo);
            if (image == null) {
                throw new IOException("Não foi possível ler a imagem: " + arquivo.getAbsolutePath());
            }
            String texto = tesseract.doOCR(image);

            ResultadoExtracaoTexto resultado = new ResultadoExtracaoTexto();
            resultado.setTipoFonte(TipoDocumentoFonte.IMAGEM);
            resultado.setTextoCompleto(texto);
            return resultado;
        } catch (TesseractException e) {
            throw new IOException("Erro ao executar OCR na imagem", e);
        }
    }

    private boolean contemPalavrasChave(String texto) {
        if (texto == null || texto.isEmpty())
            return false;
        String t = texto.toLowerCase();

        // Palavras-chave básicas
        boolean temKeywords = t.contains("placa") || t.contains("chassi") || t.contains("renavam") ||
                t.contains("marca") || t.contains("ano") || t.contains("modelo") ||
                t.contains("crlv") || t.contains("veículo") || t.contains("proprietário");

        if (!temKeywords)
            return false;

        // Validação extra: Se tem keywords, tem algum VALOR associado?
        // Se o PDF só tem "Placa:" mas não tem a placa (porque é imagem), o texto
        // extraído será curto ou só labels.
        // Vamos checar se tem algo que parece uma Placa ou Chassi ou Ano.

        // Regex simplificados para detecção rápida
        boolean temPlaca = texto.matches("(?s).*?[A-Z]{3}[0-9][0-9A-Z][0-9]{2}.*?");
        boolean temChassi = texto.matches("(?s).*?[A-Z0-9]{17}.*?");
        boolean temAno = texto.matches("(?s).*?(19|20)\\d{2}.*?");
        boolean temRenavam = texto.matches("(?s).*?[0-9]{9,11}.*?");

        // Se tiver keywords mas NENHUM valor reconhecível, provavelmente é um
        // formulário em branco ou imagem.
        // Retorna false para forçar OCR.
        return temPlaca || temChassi || temAno || temRenavam;
    }
}
