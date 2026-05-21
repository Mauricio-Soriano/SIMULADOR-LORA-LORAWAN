package mx.mauricio.lorawan.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvPreviewService {

    public Map<String, Object> buildPreview(Path csvPath, String delimiter, boolean hasHeader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        int rowCount = 0;
        int maxColumns = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rowCount++;

                if (rows.size() < 10) {
                    String[] tokens = line.split(java.util.regex.Pattern.quote(delimiter), -1);
                    List<String> row = new ArrayList<>();
                    for (String token : tokens) row.add(token.trim());
                    rows.add(row);
                    maxColumns = Math.max(maxColumns, row.size());
                }
            }
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        List<String> headerSource = hasHeader && !rows.isEmpty() ? rows.get(0) : null;

        for (int i = 0; i < maxColumns; i++) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("index", i);
            col.put("name", headerSource != null && i < headerSource.size() && !headerSource.get(i).isBlank()
                    ? headerSource.get(i)
                    : "col_" + i);
            columns.add(col);
        }

        List<List<String>> previewRows;
        if (hasHeader && rows.size() > 1) {
            previewRows = rows.subList(1, Math.min(rows.size(), 6));
        } else {
            previewRows = rows.subList(0, Math.min(rows.size(), 5));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columnCount", maxColumns);
        result.put("columns", columns);
        result.put("previewRows", previewRows);
        result.put("rowCountEstimate", rowCount);
        return result;
    }
}
