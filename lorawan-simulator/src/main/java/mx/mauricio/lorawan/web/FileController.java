package mx.mauricio.lorawan.web;

import static spark.Spark.after;
import static spark.Spark.post;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

public class FileController {

    private static final Path UPLOAD_DIR = Path.of("data", "uploads");

    public void registerRoutes() {

        after("/*", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        post("/upload", (request, response) -> {
            response.type("application/json");

            Files.createDirectories(UPLOAD_DIR);

            request.attribute(
                "org.eclipse.jetty.multipartConfig",
                new MultipartConfigElement(System.getProperty("java.io.tmpdir"))
            );

            Part filePart = request.raw().getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                response.status(400);
                return JsonUtil.toJson(new UploadResponse(false, null, "No se recibió archivo."));
            }

            String submittedName = filePart.getSubmittedFileName();
            if (submittedName == null || submittedName.isBlank()) {
                response.status(400);
                return JsonUtil.toJson(new UploadResponse(false, null, "Nombre de archivo inválido."));
            }

            String safeName = Path.of(submittedName)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

            Path destination = UPLOAD_DIR.resolve(System.currentTimeMillis() + "_" + safeName);

            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                filePart.delete();
            }

            String normalizedPath = destination.toString().replace("\\", "/");

            return JsonUtil.toJson(
                new UploadResponse(true, normalizedPath, "Archivo cargado correctamente.")
            );
        });
    }

    public static class UploadResponse {
        private boolean success;
        private String path;
        private String message;

        public UploadResponse() {
        }

        public UploadResponse(boolean success, String path, String message) {
            this.success = success;
            this.path = path;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}