package ru.vkr.ldpcapp.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextReportWriter {

    public void write(Path targetPath, String content) throws IOException {
        if (targetPath == null) {
            throw new IOException("Не указан путь для сохранения TXT-отчёта.");
        }
        if (content == null || content.isBlank()) {
            throw new IOException("Содержимое отчёта пустое. Нечего сохранять.");
        }

        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(targetPath, content, StandardCharsets.UTF_8);
    }
}
