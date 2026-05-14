package mx.mauricio.lorawan.simulator;

import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.simulator.dto.DeviceRequest;
import mx.mauricio.lorawan.simulator.dto.GatewayRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;

public class Simulator {

    public static void main(String[] args) {
        System.out.println("Starting LoRaWAN simulator (configurable runner)...");

        SimulationRequest request = new SimulationRequest();
        request.setInputFile("data/t5.csv");
        request.setRowsToProcess(100);
        request.setSendIntervalMs(150);

        GatewayRequest gateway = new GatewayRequest(
                "gw-1",
                100.0,
                50.0,
                20,
                5000,
                6000
        );
        request.setGateway(gateway);

        request.addDevice(new DeviceRequest("dev-geo", LoRaConfig.US915_CLASS_A, 1, 10.0, 20.0));
        request.addDevice(new DeviceRequest("dev-ambiental", LoRaConfig.EU868_CLASS_B, 2, 20.0, 30.0));
        request.addDevice(new DeviceRequest("dev-imu", LoRaConfig.US915_CLASS_C, 3, 40.0, 10.0));
        request.addDevice(new DeviceRequest("dev-gyro", LoRaConfig.US915_CLASS_A, 4, 15.0, 35.0));

        SimulationRunner runner = new SimulationRunner();
        SimulationResult result = runner.run(request);

        System.out.println("\n=== Resultado de simulación ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Rows processed: " + result.getRowsProcessed());
        System.out.println("Rows skipped: " + result.getRowsSkipped());
        System.out.println("Devices configured: " + result.getDevicesConfigured());
        System.out.println("Message: " + result.getMessage());
    }
}