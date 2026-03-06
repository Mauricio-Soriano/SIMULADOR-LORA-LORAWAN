package mx.mauricio.lorawan.simulator;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.NetworkServer;

public class Simulator {

    public static void main(String[] args) {
        System.out.println("Starting LoRaWAN simulator v2 (with PHY params)...");

        NetworkServer networkServer = new NetworkServer();
        Gateway gateway = new Gateway("gw-1", networkServer, 100.0, 50.0, 20);

        // Device con parámetros por defecto
        Device device1 = new Device("dev-1", gateway);
        
        // Device con SF12 (largo alcance) y payload grande
        Device device2 = new Device("dev-2", gateway, 12, 125, "4/8", 50);

        // Device rápido (SF7, BW 500kHz)
        Device device3 = new Device("dev-3", gateway, 7, 500, "4/5", 10);

        device1.sendUplink("Temp: 25C");
        device2.sendUplink("Sensor data...");
        device3.sendUplink("Quick ping");
    }
}

