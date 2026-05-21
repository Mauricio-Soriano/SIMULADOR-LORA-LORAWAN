package mx.mauricio.lorawan.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FilePreviewHandler implements HttpHandler {

    private final UploadedFileStore fileStore;
    private final MultipartParser multipartParser;
    private final CsvPreviewService csvPreviewService;

    public FilePreviewHandler(UploadedFileStore fileStore) {
        this.fileStore = fileStore;
        this.multipartParser = new MultipartParser();
        this.csvPreviewService = new CsvPreviewService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        WebUtil.addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            WebUtil.sendJson(exchange, 405, """
                {"success":false,"message":"Método no permitido"}
            """);
            return;
        }

        try {
            Map<String, MultipartParser.Part> parts = multipartParser.parse(exchange);

            MultipartParser.Part filePart = parts.get("file");
            if (filePart == null || filePart.getContent().length == 0) {
                WebUtil.sendJson(exchange, 400, """
                    {"success":false,"message":"El archivo es obligatorio"}
                """);
                return;
            }

            String delimiter = ",";
            if (parts.containsKey("delimiter") && !parts.get("delimiter").asText().isBlank()) {
                delimiter = parts.get("delimiter").asText();
            }

            boolean hasHeader = false;
            if (parts.containsKey("hasHeader")) {
                hasHeader = Boolean.parseBoolean(parts.get("hasHeader").asText());
            }

            UploadedFileStore.StoredFile stored = fileStore.save(filePart.getFileName(), filePart.getContent());
            Map<String, Object> preview = csvPreviewService.buildPreview(stored.getPath(), delimiter, hasHeader);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("fileToken", stored.getToken());
            response.put("fileName", stored.getOriginalFileName());
            response.put("sizeBytes", stored.getSizeBytes());
            response.put("delimiter", delimiter);
            response.put("hasHeader", hasHeader);
            response.putAll(preview);

            WebUtil.sendJson(exchange, 200, JsonUtil.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            WebUtil.sendJson(exchange, 500, JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Error procesando archivo: " + e.getMessage()
            )));
        }
    }
}