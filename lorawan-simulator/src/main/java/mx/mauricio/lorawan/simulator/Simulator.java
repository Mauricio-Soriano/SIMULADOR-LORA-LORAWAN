package mx.mauricio.lorawan.simulator;
//import java.io.IOException;
//import java.nio.file.Paths;

import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.NetworkServer;
import mx.mauricio.lorawan.source.FuenteInformacion;
//import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.communication.TcpGatewayServer;



public class Simulator {

public static void main(String[] args) {
    System.out.println("Starting LoRaWAN simulator v3 (with LoRaConfig)...");

    NetworkServer ns = new NetworkServer();
    Gateway gw = new Gateway("gw-1", ns, 100.0, 50.0, 20);

    // Registrar gateway en el NetworkServer
    ns.registerGateway(gw);

    //Para UDP
    UdpGatewayServer udpServer = new UdpGatewayServer(5000, gw);
    Thread serverThread = new Thread(udpServer);
    serverThread.setDaemon(true);
    serverThread.start();
    //Para TCP
    TcpGatewayServer tcpServer = new TcpGatewayServer(6000, gw);
    Thread tcpThread = new Thread(tcpServer);
    tcpThread.setDaemon(true);
    tcpThread.start();

    try {
        Thread.sleep(300);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }



    // Dispositivos con diferentes configuraciones LoRaWAN
    Device dev1 = new Device("dev-1", gw, LoRaConfig.US915_CLASS_A);
    Device dev2 = new Device("dev-2", gw, LoRaConfig.EU868_CLASS_B);  
    Device dev3 = new Device("dev-3", gw, LoRaConfig.US915_CLASS_C);
    // Registro de dispositivos en el servidor
    ns.registerDevice(dev1);
    ns.registerDevice(dev2);
    ns.registerDevice(dev3);

    // Fuente de datos
    try {
        FuenteInformacion fuente = new FuenteInformacion("data/mediciones.txt");
        fuente.cargarInformacion();

        // Enviar datos del archivo con dev1
        for (String linea : fuente.getLineas()) {
            dev1.sendUplink(new ApplicationPayload(linea, 1));
        }
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    } catch (Exception e) {
        System.err.println("Error fuente: " + e.getMessage());
    }
    // Envíos de prueba para TCP
        dev2.sendUplink(new ApplicationPayload("msg-dev2", 2));
        dev3.sendUplink(new ApplicationPayload("msg-dev3", 3));

    try {
        Thread.sleep(700);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    System.out.println("\n=== Configuraciones LoRaWAN ===");

    try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    
    System.out.printf("dev1: %s (%.1fMHz SF%d %s)%n", 
        dev1.getConfig().name(), dev1.getConfig().getFrequencyMHz(),
        dev1.getConfig().getSpreadingFactor(), dev1.getConfig().getCodingRate());
    System.out.printf("dev2: %s (%.1fMHz SF%d %s)%n", 
        dev2.getConfig().name(), dev2.getConfig().getFrequencyMHz(),
        dev2.getConfig().getSpreadingFactor(), dev2.getConfig().getCodingRate());
    System.out.printf("dev3: %s (%.1fMHz SF%d %s)%n", 
        dev3.getConfig().name(), dev3.getConfig().getFrequencyMHz(),
        dev3.getConfig().getSpreadingFactor(), dev3.getConfig().getCodingRate());

    

    }

}

