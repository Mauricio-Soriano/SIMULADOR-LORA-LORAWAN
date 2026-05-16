package mx.mauricio.lorawan.simulator;

import java.util.List;

import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.simulator.dto.DeviceRequest;
import mx.mauricio.lorawan.simulator.dto.GatewayRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;

public class Simulator {

    public static void main(String[] args) {
        System.out.println("Starting LoRaWAN simulator (configurable runner + payload mapping)...");

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

        request.addDevice(new DeviceRequest(
                "dev-geo",
                LoRaConfig.US915_CLASS_A,
                1,
                10.0,
                20.0,
                List.of(0, 1, 2, 3)
        ));

        request.addDevice(new DeviceRequest(
                "dev-ambiental",
                LoRaConfig.EU868_CLASS_B,
                2,
                20.0,
                30.0,
                List.of(4, 5, 6)
        ));

        request.addDevice(new DeviceRequest(
                "dev-imu",
                LoRaConfig.US915_CLASS_C,
                3,
                40.0,
                10.0,
                List.of(7, 8, 9)
        ));

        request.addDevice(new DeviceRequest(
                "dev-gyro",
                LoRaConfig.US915_CLASS_A,
                4,
                15.0,
                35.0,
                List.of(10, 11, 12)
        ));

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