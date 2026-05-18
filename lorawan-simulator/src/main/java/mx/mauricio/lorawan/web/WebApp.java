package mx.mauricio.lorawan.web;

import static spark.Spark.awaitInitialization;
import static spark.Spark.exception;
import static spark.Spark.init;
import static spark.Spark.ipAddress;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

public class WebApp {

    public static void main(String[] args) {
        ipAddress("0.0.0.0");
        port(8080);

        staticFiles.location("/public");

        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        SimulationController controller = new SimulationController();

        path("/api", () -> {
            path("/simulations", () -> {
                controller.registerRoutes();
            });
        });

        notFound((request, response) -> {
            response.type("application/json");
            return "{\"success\":false,\"message\":\"Ruta no encontrada.\"}";
        });

        exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            response.type("application/json");
            response.body(JsonUtil.toJson(new ApiMessage(false, "Error interno: " + exception.getMessage())));
            exception.printStackTrace();
        });

        init();
        awaitInitialization();

        System.out.println("Servidor web iniciado en http://localhost:8080/simulator-dashboard.html");
    }

    public static class ApiMessage {
        private boolean success;
        private String message;

        public ApiMessage() {
        }

        public ApiMessage(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}