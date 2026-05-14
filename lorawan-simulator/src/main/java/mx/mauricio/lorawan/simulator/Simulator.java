package mx.mauricio.lorawan.simulator;

import mx.mauricio.lorawan.communication.TcpGatewayServer;
import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.NetworkServer;
import mx.mauricio.lorawan.source.FuenteInformacion;

public class Simulator {

    public static void main(String[] args) {
        System.out.println("Starting LoRaWAN simulator vFinal (4 dispositivos por fila CSV)...");

        NetworkServer ns = new NetworkServer();
        Gateway gw = new Gateway("gw-1", ns, 100.0, 50.0, 20);
        ns.registerGateway(gw);

        UdpGatewayServer udpServer = new UdpGatewayServer(5000, gw);
        Thread udpThread = new Thread(udpServer, "udp-gateway-thread");
        udpThread.setDaemon(true);
        udpThread.start();

        TcpGatewayServer tcpServer = new TcpGatewayServer(6000, gw);
        Thread tcpThread = new Thread(tcpServer, "tcp-gateway-thread");
        tcpThread.setDaemon(true);
        tcpThread.start();

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Device devGeo = new Device("dev-geo", gw, LoRaConfig.US915_CLASS_A);
        Device devAmbiental = new Device("dev-ambiental", gw, LoRaConfig.EU868_CLASS_B);
        Device devImu = new Device("dev-imu", gw, LoRaConfig.US915_CLASS_C);
        Device devGyro = new Device("dev-gyro", gw, LoRaConfig.US915_CLASS_A);

        ns.registerDevice(devGeo);
        ns.registerDevice(devAmbiental);
        ns.registerDevice(devImu);
        ns.registerDevice(devGyro);

        int procesadas = 0;
        int enviadasGeo = 0;
        int enviadasAmbiental = 0;
        int enviadasImu = 0;
        int enviadasGyro = 0;
        int omitidas = 0;

        try {
            FuenteInformacion fuente = new FuenteInformacion("data/t5.csv");
            fuente.cargarInformacion();

            for (String linea : fuente.getLineas()) {
                if (linea == null || linea.isBlank()) {
                    continue;
                }

                String[] cols = linea.split(",", -1);

                if (cols.length < 13) {
                    System.out.println("Fila inválida, se omite: " + linea);
                    omitidas++;
                    continue;
                }

                String lat = valor(cols, 0);
                String lon = valor(cols, 1);
                String fecha = valor(cols, 2);
                String hora = valor(cols, 3);

                String temp = valor(cols, 4);
                String hum = valor(cols, 5);
                String pres = valor(cols, 6);

                String xAcel = valor(cols, 7);
                String yAcel = valor(cols, 8);
                String zAcel = valor(cols, 9);

                String xGy = valor(cols, 10);
                String yGy = valor(cols, 11);
                String zGy = valor(cols, 12);

                String payloadGeo = lat + "," + lon + "," + fecha + "," + hora;
                String payloadAmbiental = temp + "," + hum + "," + pres;
                String payloadImu = xAcel + "," + yAcel + "," + zAcel;
                String payloadGyro = xGy + "," + yGy + "," + zGy;

                devGeo.sendUplink(new ApplicationPayload(payloadGeo, 1));
                enviadasGeo++;

                devAmbiental.sendUplink(new ApplicationPayload(payloadAmbiental, 2));
                enviadasAmbiental++;

                devImu.sendUplink(new ApplicationPayload(payloadImu, 3));
                enviadasImu++;

                devGyro.sendUplink(new ApplicationPayload(payloadGyro, 4));
                enviadasGyro++;

                procesadas++;

                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error fuente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\n=== Resumen de envíos desde CSV ===");
            System.out.println("Filas procesadas: " + procesadas);
            System.out.println("Filas omitidas: " + omitidas);
            System.out.println("dev-geo enviados: " + enviadasGeo);
            System.out.println("dev-ambiental enviados: " + enviadasAmbiental);
            System.out.println("dev-imu enviados: " + enviadasImu);
            System.out.println("dev-gyro enviados: " + enviadasGyro);

            System.out.println("\n=== Configuraciones LoRaWAN ===");
            System.out.printf("dev-geo: %s (%.1fMHz SF%d %s)%n",
                    devGeo.getConfig().name(),
                    devGeo.getConfig().getFrequencyMHz(),
                    devGeo.getConfig().getSpreadingFactor(),
                    devGeo.getConfig().getCodingRate());

            System.out.printf("dev-ambiental: %s (%.1fMHz SF%d %s)%n",
                    devAmbiental.getConfig().name(),
                    devAmbiental.getConfig().getFrequencyMHz(),
                    devAmbiental.getConfig().getSpreadingFactor(),
                    devAmbiental.getConfig().getCodingRate());

            System.out.printf("dev-imu: %s (%.1fMHz SF%d %s)%n",
                    devImu.getConfig().name(),
                    devImu.getConfig().getFrequencyMHz(),
                    devImu.getConfig().getSpreadingFactor(),
                    devImu.getConfig().getCodingRate());

            System.out.printf("dev-gyro: %s (%.1fMHz SF%d %s)%n",
                    devGyro.getConfig().name(),
                    devGyro.getConfig().getFrequencyMHz(),
                    devGyro.getConfig().getSpreadingFactor(),
                    devGyro.getConfig().getCodingRate());

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("\nCerrando servidores...");
            udpServer.stop();
            tcpServer.stop();

            udpThread.interrupt();
            tcpThread.interrupt();

            try {
                udpThread.join(1000);
                tcpThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Simulation complete.");
        }
    }

    private static String valor(String[] cols, int index) {
        if (index >= cols.length) {
            return "0";
        }

        String v = cols[index];
        if (v == null) {
            return "0";
        }

        v = v.trim();
        return v.isEmpty() ? "0" : v;
    }
}