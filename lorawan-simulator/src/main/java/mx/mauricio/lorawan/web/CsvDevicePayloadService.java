package mx.mauricio.lorawan.web;

import mx.mauricio.lorawan.web.dto.SimulationRunRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvDevicePayloadService {

    public List<List<String>> readRows(Path csvPath, String delimiter, int maxRows, boolean hasHeader) throws IOException {
        List<List<String>> rows = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null && rows.size() < maxRows) {
                if (line.isBlank()) continue;

                if (hasHeader && firstLine) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] tokens = line.split(java.util.regex.Pattern.quote(delimiter), -1);
                List<String> row = new ArrayList<>();
                for (String token : tokens) row.add(token.trim());
                rows.add(row);
            }
        }

        return rows;
    }

    public String buildPayload(List<String> row, SimulationRunRequest.DeviceConfig device) {
        List<String> values = new ArrayList<>();
        if (device.columnIndexes != null) {
            for (Integer index : device.columnIndexes) {
                if (index != null && index >= 0 && index < row.size()) {
                    values.add(row.get(index));
                }
            }
        }
        return String.join(",", values);
    }
}