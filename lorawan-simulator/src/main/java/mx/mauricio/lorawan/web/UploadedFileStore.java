package mx.mauricio.lorawan.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UploadedFileStore {

    public static class StoredFile {
        private final String token;
        private final String originalFileName;
        private final Path path;
        private final long sizeBytes;

        public StoredFile(String token, String originalFileName, Path path, long sizeBytes) {
            this.token = token;
            this.originalFileName = originalFileName;
            this.path = path;
            this.sizeBytes = sizeBytes;
        }

        public String getToken() { return token; }
        public String getOriginalFileName() { return originalFileName; }
        public Path getPath() { return path; }
        public long getSizeBytes() { return sizeBytes; }
    }

    private static UploadedFileStore instance;

    private final Path uploadDir;
    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    private UploadedFileStore() {
        try {
            this.uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar UploadedFileStore", e);
        }
    }

    public static synchronized UploadedFileStore getInstance() {
        if (instance == null) {
            instance = new UploadedFileStore();
        }
        return instance;
    }

    public StoredFile save(String originalFileName, byte[] content) throws IOException {
        String token = "upload_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String safeName = sanitizeFileName(originalFileName);
        Path target = uploadDir.resolve(token + "_" + safeName);
        Files.write(target, content);

        StoredFile stored = new StoredFile(token, safeName, target, content.length);
        files.put(token, stored);
        return stored;
    }

    public StoredFile get(String token) {
        return files.get(token);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "upload.csv";
        return fileName.replace("\\", "_").replace("/", "_").replace("..", "_");
    }
}