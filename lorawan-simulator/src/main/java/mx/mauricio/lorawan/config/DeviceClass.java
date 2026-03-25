package mx.mauricio.lorawan.config;

/**
 * Clases de dispositivo LoRaWAN (A, B, C)
 */
public enum DeviceClass {
    CLASS_A("Clase A - RX1/RX2 después de TX"),
    CLASS_B("Clase B - Ping slots programados"), 
    CLASS_C("Clase C - RX continua");

    private final String description;

    DeviceClass(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
