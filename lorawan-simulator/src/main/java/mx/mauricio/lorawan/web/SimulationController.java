package mx.mauricio.lorawan.web;

import static spark.Spark.after;
import static spark.Spark.post;


import mx.mauricio.lorawan.web.dto.SimulationRunRequest;
import mx.mauricio.lorawan.web.dto.SimulationRunResponse;
import mx.mauricio.lorawan.simulator.SimulationRunner;
import mx.mauricio.lorawan.simulator.dto.SimulationRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;
import mx.mauricio.lorawan.simulator.dto.DeviceRequest;
import mx.mauricio.lorawan.simulator.dto.GatewayRequest;
import mx.mauricio.lorawan.config.LoRaConfig;




public class SimulationController {

    

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

            System.out.println("============== REQUEST ==============");
            System.out.println("inputFile = " + simulationRequest.inputFile);

            System.out.println(
                "simulation = "
                + (simulationRequest.simulation == null
                    ? "NULL"
                    : "OK")
            );

            if (simulationRequest.simulation != null) {
                System.out.println(
                    "rowsToProcess = "
                    + simulationRequest.simulation.rowsToProcess
                );

                System.out.println(
                    "sendIntervalMs = "
                    + simulationRequest.simulation.sendIntervalMs
                );
            }

            String validationError = validate(simulationRequest);
            System.out.println("validationError = " + validationError);
            if (validationError != null) {
                response.status(400);
                System.out.println("VALIDACION OK");
                return JsonUtil.toJson(new ErrorResponse(false, validationError));
            }
            long start = System.currentTimeMillis();

            SimulationRequest simulatorRequest =
                    convertRequest(
                            simulationRequest);

            SimulationRunner runner =
                    new SimulationRunner();

            SimulationResult simulatorResult =
                    runner.run(
                            simulatorRequest);

            SimulationRunResponse responseBody =
                    new SimulationRunResponse();

            responseBody.success =
                    simulatorResult.isSuccess();

            responseBody.message =
                    simulatorResult.getMessage();

            responseBody.summary.rowsProcessed =
                    simulatorResult.getRowsProcessed();

            responseBody.summary.rowsSkipped =
                    simulatorResult.getRowsSkipped();

            responseBody.summary.devicesConfigured =
                    simulatorResult.getDevicesConfigured();

            responseBody.summary.durationMs =
                    System.currentTimeMillis() - start;

            return JsonUtil.toJson(responseBody);

            //Path csvPath = Path.of(simulationRequest.inputFile);

            /*if (!Files.exists(csvPath)) {
                response.status(404);
                return JsonUtil.toJson(
                    new ErrorResponse(
                        false,
                        "Archivo no encontrado: " + simulationRequest.inputFile
                    )
                );
            }

            long start = System.currentTimeMillis();

            List<List<String>> rows = csvService.readRows(
                csvPath,
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

            return JsonUtil.toJson(result); */ 
        });
    }

    private String validate(SimulationRunRequest request) {

        if (request == null)
            return "Body JSON inválido";

        if (request.inputFile == null || request.inputFile.isBlank())
            return "inputFile es obligatorio";

        if (request.simulation == null)
            return "simulation es obligatorio";

        if (request.gateway == null)
            return "gateway es obligatorio";

        if (request.devices == null || request.devices.isEmpty())
            return "devices es obligatorio";

        if (request.simulation.rowsToProcess <= 0)
            return "rowsToProcess debe ser mayor que 0";

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

    


    private SimulationRequest convertRequest(
            SimulationRunRequest webRequest) {

        SimulationRequest simulatorRequest =
                new SimulationRequest();

        simulatorRequest.setInputFile(
                webRequest.inputFile);

        simulatorRequest.setRowsToProcess(
                webRequest.simulation.rowsToProcess);

        simulatorRequest.setSendIntervalMs(
                webRequest.simulation.sendIntervalMs);

        GatewayRequest gateway =
                new GatewayRequest();

        gateway.setGatewayId(
                webRequest.gateway.gatewayId);

        gateway.setX(
                webRequest.gateway.x);

        gateway.setY(
                webRequest.gateway.y);

        gateway.setMaxTxPowerDBm(
                (int) webRequest.gateway.maxTxPowerDBm);

        gateway.setUdpPort(
                webRequest.gateway.udpPort);

        gateway.setTcpPort(
                webRequest.gateway.tcpPort);

        simulatorRequest.setGateway(gateway);

        for (SimulationRunRequest.DeviceConfig webDevice
                : webRequest.devices) {

            DeviceRequest device =
                    new DeviceRequest();

            device.setDeviceId(
                    webDevice.deviceId);

            device.setEnabled(
                    webDevice.enabled);

            device.setFPort(
                    webDevice.fPort);

            if (webDevice.position != null) {
                device.setX(
                        webDevice.position.x);

                device.setY(
                        webDevice.position.y);
            }

            device.setColumnIndexes(
                    webDevice.columnIndexes);

            device.setConfig(
                    resolveConfig(
                            webDevice.config));

            simulatorRequest.addDevice(
                    device);
        }

        return simulatorRequest;
    }

    private LoRaConfig resolveConfig(
            String configName) {

        if (configName == null) {
            return LoRaConfig.US915_CLASS_A;
        }

        switch (configName.toUpperCase()) {

            case "US915_CLASS_A":
                return LoRaConfig.US915_CLASS_A;

            case "US915_CLASS_B":
                return LoRaConfig.US915_CLASS_B;

            case "US915_CLASS_C":
                return LoRaConfig.US915_CLASS_C;

            case "EU868_CLASS_A":
                return LoRaConfig.EU868_CLASS_A;

            case "EU868_CLASS_B":
                return LoRaConfig.EU868_CLASS_B;

            case "EU868_CLASS_C":
                return LoRaConfig.EU868_CLASS_C;

            default:
                return LoRaConfig.US915_CLASS_A;
        }
    }




}