package mx.mauricio.lorawan.simulator;
import java.io.IOException;
import java.nio.file.Paths;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.NetworkServer;
import mx.mauricio.lorawan.source.FuenteInformacion;

public class Simulator {

    public static void main(String[] args) {
        System.out.println("Starting LoRaWAN simulator v2 (with PHY params)...");


        NetworkServer networkServer = new NetworkServer();
        Gateway gateway = new Gateway("gw-1", networkServer, 100.0, 50.0, 20);

        // Device con parámetros por defecto
        Device device1 = new Device("dev-1", gateway);
        
        /*FuenteInformacion fuente = new FuenteInformacion("data/mediciones.txt");
        fuente.cargarInformacion();  // aquí ya hace la vista previa

        for (String linea : fuente.getLineas()) {
            ApplicationPayload payload = new ApplicationPayload(linea, 1);
            device1.sendUplink(payload);
        }*/
       try {
            System.out.println("Directorio actual: " + System.getProperty("user.dir"));
            //FuenteInformacion fuente = new FuenteInformacion("lorawan-simulator/data/mediciones.txt");
            FuenteInformacion fuente = new FuenteInformacion("data/mediciones.txt");
            //FuenteInformacion fuente = new FuenteInformacion("mediciones.txt");
            fuente.cargarInformacion();
    
            for (String linea : fuente.getLineas()) {
                ApplicationPayload payload = new ApplicationPayload(linea, 1);
                device1.sendUplink(payload);
            }
        } catch (IOException e) {
            System.err.println("Error cargando archivo: " + e.getMessage());
            System.err.println("Ruta absoluta que buscó: " + Paths.get("data/mediciones.txt").toAbsolutePath());
        }



        // Device con SF12 (largo alcance) y payload grande
        Device device2 = new Device("dev-2", gateway, 12, 125, "4/8", 50);

        // Device rápido (SF7, BW 500kHz)
        Device device3 = new Device("dev-3", gateway, 7, 500, "4/5", 10);

        //device1.sendUplink("Temp: 25C");
        //nuevo método con las clases del frame
        device1.sendUplink(new ApplicationPayload("Temp: 25C", 1));
        device2.sendUplink("Sensor data...");
        device3.sendUplink("Quick ping");

        
    }
}

