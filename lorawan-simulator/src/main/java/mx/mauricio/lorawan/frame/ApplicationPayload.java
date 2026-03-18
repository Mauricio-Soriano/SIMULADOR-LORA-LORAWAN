
package mx.mauricio.lorawan.frame;

/**
 * Capa de aplicación LoRaWAN - Payload de datos del usuario
 */
public class ApplicationPayload {
    private final String data;
    private final int fPort;

    public ApplicationPayload(String data, int fPort) {
        this.data = data;
        this.fPort = fPort;
    }

    public String getData() { return data; }
    public int getFPort() { return fPort; }

    @Override
    public String toString() {
        return String.format("AppPayload[fPort=%d,data='%s']", fPort, data);
    }
}

