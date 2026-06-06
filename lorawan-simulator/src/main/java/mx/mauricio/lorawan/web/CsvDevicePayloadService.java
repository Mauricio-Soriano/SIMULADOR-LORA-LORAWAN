package mx.mauricio.lorawan.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mx.mauricio.lorawan.web.dto.SimulationRunRequest;

public class CsvDevicePayloadService {

    public List<List<String>> readRows(Path csvPath, String delimiter, int maxRows, boolean hasHeader)
        throws IOException {

        List<List<String>> rows = new ArrayList<>();

        if (csvPath == null) {
            throw new IllegalArgumentException("csvPath no puede ser null");
        }

        if (!Files.exists(csvPath)) {
            throw new IOException("El archivo no existe: " + csvPath);
        }

        String effectiveDelimiter = normalizeDelimiter(delimiter);
        int effectiveMaxRows = maxRows > 0 ? maxRows : Integer.MAX_VALUE;

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null && rows.size() < effectiveMaxRows) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                if (hasHeader && firstLine) {
                    firstLine = false;
                    continue;
                }

                firstLine = false;

                String[] tokens = line.split(Pattern.quote(effectiveDelimiter), -1);
                List<String> row = new ArrayList<>(tokens.length);

                for (String token : tokens) {
                    row.add(token == null ? "" : token.trim());
                }

                rows.add(row);
            }
        }

        return rows;
    }

    public String buildPayload(List<String> row, SimulationRunRequest.DeviceConfig device) {
        if (row == null || row.isEmpty()) {
            return "";
        }

        if (device == null || device.columnIndexes == null || device.columnIndexes.isEmpty()) {
            return "";
        }

        List<String> values = new ArrayList<>();

        for (Integer index : device.columnIndexes) {
            if (index == null) {
                continue;
            }

            if (index >= 0 && index < row.size()) {
                values.add(safe(row.get(index)));
            }
        }

        return String.join(",", values);
    }

    private String normalizeDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            return ",";
        }
        return delimiter;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}