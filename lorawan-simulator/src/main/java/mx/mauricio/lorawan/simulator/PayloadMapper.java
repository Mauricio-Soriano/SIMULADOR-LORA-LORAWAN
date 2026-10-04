package mx.mauricio.lorawan.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mx.mauricio.lorawan.simulator.dto.DeviceRequest;

public class PayloadMapper {

    public String buildPayload(
        String csvLine,
        DeviceRequest request,
        String delimiter) {
        if (csvLine == null || csvLine.isBlank()) {
            return "";
        }

        String effectiveDelimiter =
                (delimiter == null || delimiter.isEmpty())
                        ? ","
                        : delimiter;

        String[] cols =
                csvLine.split(
                        Pattern.quote(effectiveDelimiter),
                        -1);
        List<String> values = new ArrayList<>();

        if (request.getColumnIndexes() == null || request.getColumnIndexes().isEmpty()) {
            return csvLine;
        }

        for (Integer index : request.getColumnIndexes()) {
            values.add(getValue(cols, index));
        }

        return String.join(",", values);
    }

    private String getValue(String[] cols, int index) {
        if (index < 0 || index >= cols.length) {
            return "0";
        }

        String value = cols[index];
        if (value == null) {
            return "0";
        }

        value = value.trim();
        return value.isEmpty() ? "0" : value;
    }
}
