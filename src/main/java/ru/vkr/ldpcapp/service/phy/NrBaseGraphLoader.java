package ru.vkr.ldpcapp.service.phy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class NrBaseGraphLoader {

    public int[][] loadBg1InfoPartShifts(int z, int expectedRows, int infoCols) {
        String resourcePath = resolveBg1ResourcePath(z);
        return loadMatrix(resourcePath, expectedRows, infoCols);
    }

    private String resolveBg1ResourcePath(int z) {
        return switch (z) {
            case 8 -> "/ru/vkr/ldpcapp/ldpc/NR_1_0_8.txt";
            case 16 -> "/ru/vkr/ldpcapp/ldpc/NR_1_0_16.txt";
            case 32 -> "/ru/vkr/ldpcapp/ldpc/NR_1_0_32.txt";
            default -> throw new IllegalArgumentException("Unsupported lifting size Z=" + z + ". Supported: 8,16,32");
        };
    }
    public int[][] loadBg2InfoPartShifts(int z, int expectedRows, int infoCols) {
        String resourcePath = resolveBg2ResourcePath(z);
        return loadMatrix(resourcePath, expectedRows, infoCols);
    }

    private String resolveBg2ResourcePath(int z) {
        return switch (z) {
            case 8 -> "/ru/vkr/ldpcapp/ldpc/NR_2_0_8.txt";
            case 16 -> "/ru/vkr/ldpcapp/ldpc/NR_2_0_16.txt";
            case 32 -> "/ru/vkr/ldpcapp/ldpc/NR_2_0_32.txt";
            default -> throw new IllegalArgumentException("Unsupported BG2 lifting size Z=" + z + ". Supported: 8,16,32");
        };
    }

    private int[][] loadMatrix(String resourcePath, int expectedRows, int expectedCols) {
        int[][] matrix = new int[expectedRows][expectedCols];

        try (InputStream is = NrBaseGraphLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Не найден ресурс base graph: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int row = 0;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    if (row >= expectedRows) {
                        throw new IllegalStateException("В " + resourcePath + " больше строк, чем expectedRows=" + expectedRows);
                    }

                    String[] tokens = splitTokens(trimmed);
                    if (tokens.length < expectedCols) {
                        throw new IllegalStateException(
                                "Строка " + (row + 1) + " в " + resourcePath +
                                        ": expected >= " + expectedCols + " значений, got " + tokens.length
                        );
                    }

                    for (int col = 0; col < expectedCols; col++) {
                        matrix[row][col] = Integer.parseInt(tokens[col].trim());
                    }

                    row++;
                }

                if (row != expectedRows) {
                    throw new IllegalStateException(
                            "В " + resourcePath + " строк=" + row + ", expectedRows=" + expectedRows
                    );
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Ошибка чтения base graph из " + resourcePath, exception);
        }

        return matrix;
    }

    private String[] splitTokens(String line) {
        if (line.contains(";")) {
            return line.split(";");
        }
        if (line.contains(",")) {
            return line.split(",");
        }
        return line.split("\\s+");
    }
    public int[][] loadBg1FullShifts(int z, int expectedRows, int totalCols) {
        String resourcePath = switch (z) {
            case 8 -> "/ru/vkr/ldpcapp/ldpc/NR_BG1_FULL_Z8.txt";
            case 16 -> "/ru/vkr/ldpcapp/ldpc/NR_BG1_FULL_Z16.txt";
            case 32 -> "/ru/vkr/ldpcapp/ldpc/NR_BG1_FULL_Z32.txt";
            default -> throw new IllegalArgumentException("Unsupported Z=" + z);
        };
        return loadMatrix(resourcePath, expectedRows, totalCols);
    }

    public int[][] loadBg2FullShifts(int z, int expectedRows, int totalCols) {
        String resourcePath = switch (z) {
            case 8 -> "/ru/vkr/ldpcapp/ldpc/NR_BG2_FULL_Z8.txt";
            case 16 -> "/ru/vkr/ldpcapp/ldpc/NR_BG2_FULL_Z16.txt";
            case 32 -> "/ru/vkr/ldpcapp/ldpc/NR_BG2_FULL_Z32.txt";
            default -> throw new IllegalArgumentException("Unsupported Z=" + z);
        };
        return loadMatrix(resourcePath, expectedRows, totalCols);
    }
}