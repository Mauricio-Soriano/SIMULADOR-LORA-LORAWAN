package mx.mauricio.lorawan.gateway;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.network.NetworkServer;



public class Gateway {

    private final String gatewayId;
    private final NetworkServer networkServer;

    // Parámetros del Gateway
    private final double x, y;        // Coordenadas para distancia
    private final int maxTxPowerDBm;  // Potencia máxima de transmisión

    public Gateway(String gatewayId, NetworkServer networkServer) {
        this(gatewayId, networkServer, 0.0, 0.0, 20); // Centro por defecto
    }

    public Gateway(String gatewayId, NetworkServer networkServer, 
                   double x, double y, int maxTxPowerDBm) {
        this.gatewayId = gatewayId;
        this.networkServer = networkServer;
        this.x = x;
        this.y = y;
        this.maxTxPowerDBm = maxTxPowerDBm;
    }

    public void receiveUdpMessage(String payload) {
        System.out.printf("[Gateway %s @%.1f,%.1f] Rx UDP payload%n", gatewayId, x, y);
        networkServer.receiveFromGateway(gatewayId, payload);
    }


    public String getGatewayId() { return gatewayId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getMaxTxPowerDBm() { return maxTxPowerDBm; }

    public void receiveFromDevice(Device device, String payload) {
        System.out.println("[Gateway " + gatewayId + " @" + x + "," + y 
                + "] Rx from " + device.getDeviceId() 
                + " (SF" + device.getSpreadingFactor() + ")");
        networkServer.handleUplink(device, this, payload);
    }
}



