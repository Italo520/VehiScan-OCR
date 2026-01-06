package com.automacao.ocr.ocr;

import java.io.File;
import java.io.IOException;

public interface TesseractService {
    ResultadoExtracaoTexto extrairTexto(File arquivo) throws IOException;
}
