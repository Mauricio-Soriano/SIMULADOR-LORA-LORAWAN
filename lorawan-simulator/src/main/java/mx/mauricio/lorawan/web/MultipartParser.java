package mx.mauricio.lorawan.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MultipartParser {

    public static class Part {
        private final String name;
        private final String fileName;
        private final byte[] content;

        public Part(String name, String fileName, byte[] content) {
            this.name = name;
            this.fileName = fileName;
            this.content = content;
        }

        public String getName() { return name; }
        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }

        public String asText() {
            return new String(content, StandardCharsets.UTF_8).trim();
        }
    }

    public Map<String, Part> parse(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("multipart/form-data")) {
            throw new IOException("Content-Type no soportado");
        }

        String boundary = extractBoundary(contentType);
        if (boundary == null || boundary.isBlank()) {
            throw new IOException("Boundary no encontrado");
        }

        byte[] body = exchange.getRequestBody().readAllBytes();
        String raw = new String(body, StandardCharsets.ISO_8859_1);

        String[] parts = raw.split("--" + java.util.regex.Pattern.quote(boundary));
        Map<String, Part> result = new HashMap<>();

        for (String part : parts) {
            if (part == null || part.isBlank() || part.equals("--") || part.equals("--\r\n")) {
                continue;
            }

            int headerEnd = part.indexOf("\r\n\r\n");
            if (headerEnd < 0) continue;

            String headersBlock = part.substring(0, headerEnd);
            String bodyBlock = part.substring(headerEnd + 4);

            if (bodyBlock.endsWith("\r\n")) {
                bodyBlock = bodyBlock.substring(0, bodyBlock.length() - 2);
            }
            if (bodyBlock.endsWith("--")) {
                bodyBlock = bodyBlock.substring(0, bodyBlock.length() - 2);
            }

            String disposition = null;
            for (String headerLine : headersBlock.split("\r\n")) {
                if (headerLine.toLowerCase().startsWith("content-disposition:")) {
                    disposition = headerLine;
                    break;
                }
            }

            if (disposition == null) continue;

            String name = extractDispositionValue(disposition, "name");
            String filename = extractDispositionValue(disposition, "filename");

            byte[] content = bodyBlock.getBytes(StandardCharsets.ISO_8859_1);
            result.put(name, new Part(name, filename, content));
        }

        return result;
    }

    private String extractBoundary(String contentType) {
        for (String token : contentType.split(";")) {
            String trimmed = token.trim();
            if (trimmed.startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).replace("\"", "");
            }
        }
        return null;
    }

    private String extractDispositionValue(String disposition, String key) {
        String pattern = key + "=\"";
        int start = disposition.indexOf(pattern);
        if (start < 0) return null;
        int valueStart = start + pattern.length();
        int valueEnd = disposition.indexOf("\"", valueStart);
        if (valueEnd < 0) return null;
        return disposition.substring(valueStart, valueEnd);
    }
}