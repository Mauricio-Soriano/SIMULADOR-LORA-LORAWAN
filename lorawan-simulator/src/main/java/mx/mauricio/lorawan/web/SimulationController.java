package mx.mauricio.lorawan.web;

import static spark.Spark.after;
import static spark.Spark.post;

import mx.mauricio.lorawan.communication.TcpSender;
import mx.mauricio.lorawan.communication.UdpSender;
import mx.mauricio.lorawan.web.dto.SimulationRunRequest;
import mx.mauricio.lorawan.web.dto.SimulationRunResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SimulationController {

    private final UploadedFileStore fileStore = UploadedFileStore.getInstance();
    private final CsvDevicePayloadService csvService = new CsvDevicePayloadService();

    public void registerRoutes() {
        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        post("/run", (request, response) -> {
            response.type("application/json");

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
                    ",",
                    simulationRequest.simulation.rowsToProcess,
                    false
            );

            SimulationRunResponse result = new SimulationRunResponse();
            result.success = true;
            result.simulationId = "sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            result.message = "Simulación ejecutada correctamente.";
            result.summary.rowsProcessed = rows.size();
            result.summary.devicesConfigured = simulationRequest.devices.size();

            List<SimulationRunResponse.DeviceResult> deviceResults = initDeviceResults(simulationRequest.devices);

            for (List<String> row : rows) {
                for (int i = 0; i < simulationRequest.devices.size(); i++) {
                    SimulationRunRequest.DeviceConfig device = simulationRequest.devices.get(i);
                    SimulationRunResponse.DeviceResult deviceResult = deviceResults.get(i);

                    if (!device.enabled) continue;

                    String payload = csvService.buildPayload(row, device);

                    try {
                        String frame =
                                "MHDR=40" +
                                "|DEV=" + device.deviceId +
                                "|FCNT=0001" +
                                "|FPORT=" + device.fPort +
                                "|DATA=" + payload +
                                "|MIC=0000";

                        if ("UDP".equalsIgnoreCase(device.transport)) {
                            UdpSender sender = new UdpSender("127.0.0.1", simulationRequest.gateway.udpPort);
                            sender.send(frame);
                        } else if ("TCP".equalsIgnoreCase(device.transport)) {
                            TcpSender sender = new TcpSender("127.0.0.1", simulationRequest.gateway.tcpPort);
                            sender.send(frame);
                        }

                        deviceResult.rowsProcessed++;
                        deviceResult.uplinksSent++;
                        deviceResult.lastPayload = payload;
                        result.summary.uplinksSent++;

                        SimulationRunResponse.EventItem event = new SimulationRunResponse.EventItem();
                        event.timestamp = Instant.now().toString();
                        event.level = "INFO";
                        event.deviceId = device.deviceId;
                        event.message = "Payload enviado: " + payload;
                        result.events.add(event);

                        if (simulationRequest.simulation.sendIntervalMs > 0) {
                            Thread.sleep(simulationRequest.simulation.sendIntervalMs);
                        }

                    } catch (Exception ex) {
                        deviceResult.rowsProcessed++;
                        deviceResult.uplinksFailed++;
                        result.summary.uplinksFailed++;

                        SimulationRunResponse.EventItem event = new SimulationRunResponse.EventItem();
                        event.timestamp = Instant.now().toString();
                        event.level = "ERROR";
                        event.deviceId = device.deviceId;
                        event.message = "Error enviando payload: " + ex.getMessage();
                        result.events.add(event);
                    }
                }
            }

            result.devices.addAll(deviceResults);
            result.summary.durationMs = System.currentTimeMillis() - start;

            return JsonUtil.toJson(result);
        });
    }

    private List<SimulationRunResponse.DeviceResult> initDeviceResults(List<SimulationRunRequest.DeviceConfig> devices) {
        List<SimulationRunResponse.DeviceResult> results = new ArrayList<>();
        for (SimulationRunRequest.DeviceConfig device : devices) {
            SimulationRunResponse.DeviceResult item = new SimulationRunResponse.DeviceResult();
            item.deviceId = device.deviceId;
            item.deviceClass = device.deviceClass;
            item.transport = device.transport;
            item.columnIndexes = device.columnIndexes;
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

    private String validate(SimulationRunRequest request) {
        if (request == null) return "Body JSON inválido";
        if (request.fileToken == null || request.fileToken.isBlank()) return "fileToken es obligatorio";
        if (request.simulation == null) return "simulation es obligatorio";
        if (request.gateway == null) return "gateway es obligatorio";
        if (request.devices == null || request.devices.isEmpty()) return "devices es obligatorio";
        if (request.simulation.rowsToProcess <= 0) return "rowsToProcess debe ser mayor que 0";
        return null;
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