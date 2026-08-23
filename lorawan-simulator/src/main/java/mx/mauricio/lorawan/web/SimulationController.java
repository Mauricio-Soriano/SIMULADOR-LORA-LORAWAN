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
import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters;




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
        
        simulatorRequest.setAdrEnabled(
                webRequest.adrEnabled != null
                        ? webRequest.adrEnabled
                        : true);

        simulatorRequest.setRandomLossEnabled(
                webRequest.randomLossEnabled != null
                        ? webRequest.randomLossEnabled
                        : false);

        simulatorRequest.setRandomLossProbability(
                webRequest.randomLossProbability != null
                        ? webRequest.randomLossProbability
                        : 0.0);
        simulatorRequest.setScenarioName(
                webRequest.scenarioName != null
                        ? webRequest.scenarioName
                        : "Escenario personalizado");

        Cost231LinkBudgetParameters linkBudgetParameters =
                webRequest.linkBudgetParameters != null
                        ? webRequest.linkBudgetParameters
                        : new Cost231LinkBudgetParameters();

        simulatorRequest.setLinkBudgetParameters(
                linkBudgetParameters);

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