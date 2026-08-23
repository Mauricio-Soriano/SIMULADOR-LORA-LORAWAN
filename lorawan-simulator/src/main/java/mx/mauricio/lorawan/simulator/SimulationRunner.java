package mx.mauricio.lorawan.simulator;

import java.util.ArrayList;
import java.util.List;

import mx.mauricio.lorawan.communication.TcpGatewayServer;
import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.metrics.MetricsReporter;
import mx.mauricio.lorawan.network.NetworkServer;
import mx.mauricio.lorawan.simulator.dto.DeviceRequest;
import mx.mauricio.lorawan.simulator.dto.GatewayRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;
import mx.mauricio.lorawan.source.FuenteInformacion;
import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters;

import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters;

public class SimulationRunner {

    public SimulationResult run(SimulationRequest request) {

        System.out.println(
                "[Scenario] "
                + (request.getScenarioName() != null
                        ? request.getScenarioName()
                        : "Escenario personalizado"));

        validateRequest(request);

        NetworkServer networkServer = new NetworkServer();
        boolean adrEnabled =
                request.getAdrEnabled() != null
                        ? request.getAdrEnabled()
                        : true;

        boolean randomLossEnabled =
                request.getRandomLossEnabled() != null
                        ? request.getRandomLossEnabled()
                        : false;

        double randomLossProbability =
                request.getRandomLossProbability() != null
                        ? request.getRandomLossProbability()
                        : 0.0;

        networkServer.configureAdr(
                adrEnabled);

        if (randomLossProbability < 0.0
            || randomLossProbability > 1.0) {

        throw new IllegalArgumentException(
                "randomLossProbability debe estar entre 0.0 y 1.0");
    }

        networkServer.configureRandomPacketLoss(
                randomLossEnabled,
                randomLossProbability);
        Cost231LinkBudgetParameters linkBudgetParameters =
            request.getLinkBudgetParameters() != null
                    ? request.getLinkBudgetParameters()
                    : new Cost231LinkBudgetParameters();

        System.out.println(
                "[DEBUG REQUEST] linkBudgetParameters null? "
                + (request.getLinkBudgetParameters() == null));

        System.out.println(
                "[DEBUG REQUEST] los="
                + linkBudgetParameters.isLos()
                + " frequencyMHz="
                + linkBudgetParameters.getFrequencyMHz()
                + " hb="
                + linkBudgetParameters.getHbMeters()
                + " hr="
                + linkBudgetParameters.getHrMeters()
                + " streetWidth="
                + linkBudgetParameters.getStreetWidthMeters()
                + " buildingSeparation="
                + linkBudgetParameters.getBuildingSeparationMeters()
                + " ka="
                + linkBudgetParameters.getKaDb()
                + " kd="
                + linkBudgetParameters.getKdDb()
                + " kf="
                + linkBudgetParameters.getKfDb()
                + " lbsh="
                + linkBudgetParameters.getLbshDb());

        networkServer.configureLinkBudgetParameters(
                linkBudgetParameters);
        

        GatewayRequest gwRequest = request.getGateway();

        Gateway gateway = new Gateway(
            gwRequest.getGatewayId(),
            networkServer,
            gwRequest.getX(),
            gwRequest.getY(),
            gwRequest.getMaxTxPowerDBm()
        );

        networkServer.registerGateway(gateway);

        UdpGatewayServer udpServer = new UdpGatewayServer(gwRequest.getUdpPort(), gateway);
        Thread udpThread = new Thread(udpServer, "udp-gateway-thread");
        udpThread.setDaemon(true);
        udpThread.start();

        TcpGatewayServer tcpServer = new TcpGatewayServer(gwRequest.getTcpPort(), gateway);
        Thread tcpThread = new Thread(tcpServer, "tcp-gateway-thread");
        tcpThread.setDaemon(true);
        tcpThread.start();

        sleepSilently(300);

        List<Device> devices = new ArrayList<>();
        List<DeviceRequest> enabledDevices = new ArrayList<>();
        PayloadMapper payloadMapper = new PayloadMapper();

        for (DeviceRequest deviceRequest : request.getDevices()) {
            if (deviceRequest == null || !deviceRequest.isEnabled()) {
                continue;
            }

            LoRaConfig config = deviceRequest.getConfig();
            if (config == null) {
                throw new IllegalArgumentException(
                    "El dispositivo " + deviceRequest.getDeviceId() + " no tiene un LoRaConfig válido."
                );
            }

            Device device = new Device(
                deviceRequest.getDeviceId(),
                gateway,
                config                
            );

            switch (config.getDeviceClass()) {

                case CLASS_B:
                case CLASS_C:
                    device.setConfirmed(true);
                    break;

                default:
                    device.setConfirmed(false);
            }


            devices.add(device);
            enabledDevices.add(deviceRequest);
            networkServer.registerDevice(device);
        }

        int processedRows = 0;
        int skippedRows = 0;

        try {
            FuenteInformacion fuente = new FuenteInformacion(request.getInputFile());
            fuente.cargarInformacion();

            List<String> lineas = fuente.getLineas();
            int startIndex = detectStartIndex(lineas);
            int availableRows = Math.max(0, lineas.size() - startIndex);
            int maxRows = request.getRowsToProcess() > 0
                ? Math.min(request.getRowsToProcess(), availableRows)
                : availableRows;

            for (int i = 0; i < maxRows; i++) {
                String linea = lineas.get(startIndex + i);

                if (linea == null || linea.isBlank()) {
                    skippedRows++;
                    continue;
                }

                for (Device device : devices) {
                    DeviceRequest deviceRequest = findRequestByDeviceId(enabledDevices, device.getDeviceId());

                    if (deviceRequest == null) {
                        continue;
                    }

                    String payload = payloadMapper.buildPayload(linea, deviceRequest);
                    device.sendUplink(new ApplicationPayload(payload, deviceRequest.getFPort()));
                }

                processedRows++;
                sleepSilently(request.getSendIntervalMs());
            }

            return new SimulationResult(
                true,
                processedRows,
                skippedRows,
                devices.size(),
                "Simulación ejecutada correctamente."
            );

        } catch (Exception e) {
            e.printStackTrace();

            return new SimulationResult(
                false,
                processedRows,
                skippedRows,
                devices.size(),
                "Error durante la simulación: " + e.getMessage()
            );

        } finally {
            System.out.println("\nCerrando servidores...");
            udpServer.stop();
            tcpServer.stop();

            udpThread.interrupt();
            tcpThread.interrupt();

            joinSilently(udpThread, 1000);
            joinSilently(tcpThread, 1000);

            MetricsReporter reporter =
                    new MetricsReporter();

            reporter.printReport(
                    networkServer
                            .getSessionRegistry()
                            .getAllSessions());

            System.out.println("Simulation complete.");
        }
    }

    private void validateRequest(SimulationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("SimulationRequest no puede ser null.");
        }

        if (request.getGateway() == null) {
            throw new IllegalArgumentException("La configuración del gateway es obligatoria.");
        }

        if (request.getInputFile() == null || request.getInputFile().isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo de entrada es obligatoria.");
        }

        if (request.getDevices() == null || request.getDevices().isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un dispositivo.");
        }
    }

    private DeviceRequest findRequestByDeviceId(List<DeviceRequest> requests, String deviceId) {
        for (DeviceRequest request : requests) {
            if (request.getDeviceId().equals(deviceId)) {
                return request;
            }
        }
        return null;
    }

    private int detectStartIndex(List<String> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return 0;
        }

        String firstLine = lineas.get(0);
        if (firstLine != null && firstLine.toLowerCase().contains("lat")
            && firstLine.toLowerCase().contains("temp")) {
            return 1;
        }

        return 0;
    }

    private void sleepSilently(int millis) {
        try {
            Thread.sleep(Math.max(millis, 0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void joinSilently(Thread thread, long timeoutMs) {
        try {
            thread.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}