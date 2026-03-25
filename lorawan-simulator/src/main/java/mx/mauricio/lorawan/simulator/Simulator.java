package mx.mauricio.lorawan.simulator;
//import java.io.IOException;
//import java.nio.file.Paths;

import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.NetworkServer;
import mx.mauricio.lorawan.source.FuenteInformacion;

public class Simulator {

public static void main(String[] args) {
    System.out.println("Starting LoRaWAN simulator v3 (with LoRaConfig)...");

    // Configuración
    NetworkServer ns = new NetworkServer();
    Gateway gw = new Gateway("gw-1", ns, 100.0, 50.0,20);

    // Dispositivos con diferentes configuraciones LoRaWAN
    Device dev1 = new Device("dev-1", gw, LoRaConfig.US915_CLASS_A);
    Device dev2 = new Device("dev-2", gw, LoRaConfig.EU868_CLASS_B);  
    Device dev3 = new Device("dev-3", gw, LoRaConfig.US915_CLASS_C);

    // Fuente de datos
    try {
        FuenteInformacion fuente = new FuenteInformacion("data/mediciones.txt");
        fuente.cargarInformacion();

        // Enviar datos del archivo con dev1
        for (String linea : fuente.getLineas()) {
            dev1.sendUplink(new ApplicationPayload(linea, 1));
        }
    } catch (Exception e) {
        System.err.println("Error fuente: " + e.getMessage());
    }

    System.out.println("\n=== Configuraciones LoRaWAN ===");
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

