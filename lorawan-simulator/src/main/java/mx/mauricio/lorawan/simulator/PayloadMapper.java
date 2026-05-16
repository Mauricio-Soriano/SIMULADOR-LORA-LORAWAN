package mx.mauricio.lorawan.simulator;

import java.util.ArrayList;
import java.util.List;

import mx.mauricio.lorawan.simulator.dto.DeviceRequest;

public class PayloadMapper {

    public String buildPayload(String csvLine, DeviceRequest request) {
        if (csvLine == null || csvLine.isBlank()) {
            return "";
        }

        String[] cols = csvLine.split(",", -1);
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
