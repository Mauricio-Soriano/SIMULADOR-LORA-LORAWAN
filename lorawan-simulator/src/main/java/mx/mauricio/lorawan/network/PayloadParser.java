package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

public class PayloadParser {

    public Map<String, String> parse(String payload) {

        Map<String, String> fields = new HashMap<>();

        if (payload == null || payload.isBlank()) {
            return fields;
        }

        String[] parts = payload.split("\\|");

        for (String part : parts) {

            String[] keyValue = part.split("=", 2);

            if (keyValue.length == 2) {
                fields.put(keyValue[0], keyValue[1]);
            }
        }

        return fields;
    }

    public boolean isValid(Map<String, String> fields) {

        if (!fields.containsKey("MHDR")) return false;
        if (!fields.containsKey("DEV")) return false;
        if (!fields.containsKey("FCNT")) return false;
        if (!fields.containsKey("FPORT")) return false;
        if (!fields.containsKey("DATA")) return false;
        if (!fields.containsKey("MIC")) return false;

        String mhdr = fields.get("MHDR");

        boolean validMhdr =
                "40".equals(mhdr)
                || "80".equals(mhdr);

        if (!validMhdr) {
            return false;
        }

        if (isBlank(fields.get("DEV"))) return false;
        if (isBlank(fields.get("FCNT"))) return false;
        if (isBlank(fields.get("FPORT"))) return false;
        if (fields.get("DATA") == null) return false;
        if (isBlank(fields.get("MIC"))) return false;

        return true;
    }

    public void logFields(Map<String, String> fields) {

        System.out.println("[NetworkServer] Campos parseados:");

        for (Map.Entry<String, String> entry : fields.entrySet()) {

            System.out.println(
                    " "
                    + entry.getKey()
                    + " = "
                    + entry.getValue()
            );
        }
    }

    public String describeDecodedSource(String fPort, String data) {

        String sourceType;

        switch (fPort) {

            case "1":
                sourceType = "medición de sensor";
                break;

            case "2":
                sourceType = "mensaje de estado";
                break;

            case "3":
                sourceType = "mensaje de control/prueba";
                break;

            default:
                sourceType = isNumeric(data)
                        ? "medición numérica"
                        : "mensaje de aplicación";
                break;
        }

        return sourceType
                + " (FPORT "
                + fPort
                + ") = "
                + data;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isNumeric(String value) {

        if (isBlank(value)) {
            return false;
        }

        try {

            Double.parseDouble(value);
            return true;

        } catch (NumberFormatException e) {

            return false;
        }
    }
}
