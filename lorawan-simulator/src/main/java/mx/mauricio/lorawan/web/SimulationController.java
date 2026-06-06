package mx.mauricio.lorawan.web;

import static spark.Spark.after;
import static spark.Spark.options;
import static spark.Spark.post;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mx.mauricio.lorawan.communication.TcpSender;
import mx.mauricio.lorawan.communication.UdpSender;
import mx.mauricio.lorawan.web.dto.SimulationRunRequest;
import mx.mauricio.lorawan.web.dto.SimulationRunResponse;

public class SimulationController {

    private final UploadedFileStore fileStore = UploadedFileStore.getInstance();
    private final CsvDevicePayloadService csvService = new CsvDevicePayloadService();

    public void registerRoutes() {
        enableCors();

        post("/run", (request, response) -> {
            response.type("application/json");

            try {
                SimulationRunRequest simulationRequest =
                    JsonUtil.fromJson(request.body(), SimulationRunRequest.class);

                String validationError = validate(simulationRequest);
                if (validationError != null) {
                    response.status(400);
                    return JsonUtil.toJson(new ErrorResponse(false, validationError));
                }

                UploadedFileStore.StoredFile stored = fileStore.get(simulationRequest.fileToken);
                if (stored == null) {
                    response.status(404);
                    return JsonUtil.toJson(new ErrorResponse(false, "fileToken no encontrado"));
                }

                long start = System.currentTimeMillis();

                List<List<String>> rows = csvService.readRows(
                    stored.getPath(),
                    simulationRequest.delimiter,
                    simulationRequest.simulation.rowsToProcess,
                    simulationRequest.hasHeader
                );

                SimulationRunResponse result = new SimulationRunResponse();
                result.success = true;
                result.simulationId = "sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                result.message = "Simulación ejecutada correctamente.";

                if (result.summary != null) {
                    result.summary.rowsProcessed = rows.size();
                    result.summary.devicesConfigured = countEnabledDevices(simulationRequest.devices);
                    result.summary.uplinksSent = 0;
                    result.summary.uplinksFailed = 0;
                    result.summary.durationMs = 0L;
                }

                if (result.events == null) {
                    result.events = new ArrayList<>();
                }

                if (result.devices == null) {
                    result.devices = new ArrayList<>();
                }

                List<SimulationRunResponse.DeviceResult> deviceResults =
                    initDeviceResults(simulationRequest.devices);

                for (List<String> row : rows) {
                    for (int i = 0; i < simulationRequest.devices.size(); i++) {
                        SimulationRunRequest.DeviceConfig device = simulationRequest.devices.get(i);
                        SimulationRunResponse.DeviceResult deviceResult = deviceResults.get(i);

                        if (device == null || !device.enabled) {
                            continue;
                        }

                        String payload = csvService.buildPayload(row, device);

                        try {
                            String frame = buildFrame(device.deviceId, device.fPort, payload);
                            sendFrame(device.transport, frame, simulationRequest);

                            deviceResult.rowsProcessed++;
                            deviceResult.uplinksSent++;
                            deviceResult.lastPayload = payload;

                            if (result.summary != null) {
                                result.summary.uplinksSent++;
                            }

                            result.events.add(buildEvent(
                                "INFO",
                                device.deviceId,
                                "Payload enviado: " + payload
                            ));

                            int delay = simulationRequest.simulation.sendIntervalMs;
                            if (delay > 0) {
                                Thread.sleep(delay);
                            }

                        } catch (Exception ex) {
                            deviceResult.rowsProcessed++;
                            deviceResult.uplinksFailed++;

                            if (result.summary != null) {
                                result.summary.uplinksFailed++;
                            }

                            result.events.add(buildEvent(
                                "ERROR",
                                device.deviceId,
                                "Error enviando payload: " + ex.getMessage()
                            ));
                        }
                    }
                }

                result.devices.addAll(deviceResults);

                if (result.summary != null) {
                    result.summary.durationMs = System.currentTimeMillis() - start;
                }

                return JsonUtil.toJson(result);

            } catch (Exception ex) {
                response.status(500);
                return JsonUtil.toJson(new ErrorResponse(
                    false,
                    "Error interno en /run: " + ex.getMessage()
                ));
            }
        });
    }

    private void enableCors() {
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

        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });
    }

    private String buildFrame(String deviceId, int fPort, String payload) {
        return "MHDR=40"
            + "|DEV=" + safe(deviceId)
            + "|FCNT=0001"
            + "|FPORT=" + fPort
            + "|DATA=" + safe(payload)
            + "|MIC=0000";
    }

    private void sendFrame(String transport, String frame, SimulationRunRequest simulationRequest) throws Exception {
        if ("UDP".equalsIgnoreCase(transport)) {
            UdpSender sender = new UdpSender("127.0.0.1", simulationRequest.gateway.udpPort);
            sender.send(frame);
            return;
        }

        if ("TCP".equalsIgnoreCase(transport)) {
            TcpSender sender = new TcpSender("127.0.0.1", simulationRequest.gateway.tcpPort);
            sender.send(frame);
            return;
        }

        throw new IllegalArgumentException("Transporte no soportado: " + transport);
    }

    private SimulationRunResponse.EventItem buildEvent(String level, String deviceId, String message) {
        SimulationRunResponse.EventItem event = new SimulationRunResponse.EventItem();
        event.timestamp = Instant.now().toString();
        event.level = level;
        event.deviceId = deviceId;
        event.message = message;
        return event;
    }

    private List<SimulationRunResponse.DeviceResult> initDeviceResults(
        List<SimulationRunRequest.DeviceConfig> devices
    ) {
        List<SimulationRunResponse.DeviceResult> results = new ArrayList<>();

        if (devices == null) {
            return results;
        }

        for (SimulationRunRequest.DeviceConfig device : devices) {
            SimulationRunResponse.DeviceResult item = new SimulationRunResponse.DeviceResult();
            item.deviceId = device != null ? device.deviceId : "";
            item.deviceClass = device != null ? device.deviceClass : "";
            item.transport = device != null ? device.transport : "";
            item.columnIndexes = device != null ? device.columnIndexes : new ArrayList<>();
            item.rowsProcessed = 0;
            item.uplinksSent = 0;
            item.uplinksFailed = 0;
            item.downlinksReceived = 0;
            item.lastPayload = "";
            item.avgMarginDb = 0.0;
            results.add(item);
        }

        return results;
    }

    private int countEnabledDevices(List<SimulationRunRequest.DeviceConfig> devices) {
        if (devices == null) {
            return 0;
        }

        int count = 0;
        for (SimulationRunRequest.DeviceConfig device : devices) {
            if (device != null && device.enabled) {
                count++;
            }
        }
        return count;
    }

    private String validate(SimulationRunRequest request) {
        if (request == null) {
            return "Body JSON inválido";
        }

        if (isBlank(request.fileToken)) {
            return "fileToken es obligatorio";
        }

        if (request.simulation == null) {
            return "simulation es obligatorio";
        }

        if (request.gateway == null) {
            return "gateway es obligatorio";
        }

        if (request.devices == null || request.devices.isEmpty()) {
            return "devices es obligatorio";
        }

        if (request.simulation.rowsToProcess <= 0) {
            return "rowsToProcess debe ser mayor que 0";
        }

        if (request.simulation.sendIntervalMs < 0) {
            return "sendIntervalMs no puede ser negativo";
        }

        if (isBlank(request.gateway.gatewayId)) {
            return "gateway.gatewayId es obligatorio";
        }

        if (request.gateway.udpPort <= 0) {
            return "gateway.udpPort inválido";
        }

        if (request.gateway.tcpPort <= 0) {
            return "gateway.tcpPort inválido";
        }

        boolean hasEnabledDevice = false;

        for (int i = 0; i < request.devices.size(); i++) {
            SimulationRunRequest.DeviceConfig device = request.devices.get(i);

            if (device == null) {
                return "devices[" + i + "] es nulo";
            }

            if (!device.enabled) {
                continue;
            }

            hasEnabledDevice = true;

            if (isBlank(device.deviceId)) {
                return "devices[" + i + "].deviceId es obligatorio";
            }

            if (isBlank(device.deviceClass)) {
                return "devices[" + i + "].deviceClass es obligatorio";
            }

            if (isBlank(device.transport)) {
                return "devices[" + i + "].transport es obligatorio";
            }

            if (!"UDP".equalsIgnoreCase(device.transport) && !"TCP".equalsIgnoreCase(device.transport)) {
                return "devices[" + i + "].transport debe ser UDP o TCP";
            }

            if (device.fPort <= 0) {
                return "devices[" + i + "].fPort inválido";
            }

            if (device.columnIndexes == null || device.columnIndexes.isEmpty()) {
                return "devices[" + i + "].columnIndexes debe contener al menos una columna";
            }
        }

        if (!hasEnabledDevice) {
            return "Debe existir al menos un dispositivo habilitado";
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    static class ErrorResponse {
        boolean success;
        String message;

        ErrorResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}