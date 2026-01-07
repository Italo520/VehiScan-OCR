package com.automacao.auditoria.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LocalFileService {

    private static final String[] EXTENSOES_SUPORTADAS = { ".pdf", ".jpg", ".jpeg", ".png" };

    public List<File> listarArquivos(File diretorio) {
        if (diretorio == null || !diretorio.isDirectory()) {
            return Collections.emptyList();
        }

        File[] arquivos = diretorio.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            for (String ext : EXTENSOES_SUPORTADAS) {
                if (lowerName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        });

        if (arquivos == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(arquivos)
                .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified())) // Mais recentes primeiro
                .collect(Collectors.toList());
    }
}
