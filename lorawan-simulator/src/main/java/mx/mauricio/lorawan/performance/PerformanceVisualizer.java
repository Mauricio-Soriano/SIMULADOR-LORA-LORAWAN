package mx.mauricio.lorawan.performance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PerformanceVisualizer {

    private PerformanceVisualizer() {
    }

    public static String buildCsv(
            int gatewaysRegistrados,
            int dispositivosRegistrados,
            int uplinksEnviados,
            String nota
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("registered_gateways,").append(gatewaysRegistrados).append("\n");
        sb.append("registered_devices,").append(dispositivosRegistrados).append("\n");
        sb.append("uplinks_sent,").append(uplinksEnviados).append("\n");
        sb.append("note,").append(escapeCsv(nota)).append("\n");
        return sb.toString();
    }

    public static void saveCsv(Path outputPath, String csvContent) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, csvContent);
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
