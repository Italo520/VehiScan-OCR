package com.automacao.ocr;

import java.io.File;
import java.io.IOException;

public interface ExtratorTexto {
    ResultadoExtracaoTexto extrairTexto(File arquivo) throws IOException;
}
