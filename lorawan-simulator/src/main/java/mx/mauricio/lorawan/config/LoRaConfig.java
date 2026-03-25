package mx.mauricio.lorawan.config;


/**
 * Configuraciones LoRaWAN predefinidas por región/clase
 */
public enum LoRaConfig {
    // US915 (902-928 MHz)
    US915_CLASS_A(915.0, 7, "4/5", DeviceClass.CLASS_A),
    US915_CLASS_B(915.0, 10, "4/8", DeviceClass.CLASS_B),
    US915_CLASS_C(915.0, 7, "4/5", DeviceClass.CLASS_C),

    // EU868 (863-870 MHz)
    EU868_CLASS_A(868.1, 12, "4/8", DeviceClass.CLASS_A),
    EU868_CLASS_B(868.1, 9, "4/7", DeviceClass.CLASS_B),
    EU868_CLASS_C(868.1, 7, "4/5", DeviceClass.CLASS_C),

    // AS923 (Asia)
    AS923_CLASS_A(923.2, 8, "4/6", DeviceClass.CLASS_A);

    private final double frequencyMHz;
    private final int spreadingFactor;
    private final String codingRate;
    private final DeviceClass deviceClass;

    LoRaConfig(double frequencyMHz, int spreadingFactor, 
               String codingRate, DeviceClass deviceClass) {
        this.frequencyMHz = frequencyMHz;
        this.spreadingFactor = spreadingFactor;
        this.codingRate = codingRate;
        this.deviceClass = deviceClass;
    }

    // Getters
    public double getFrequencyMHz() { return frequencyMHz; }
    public int getSpreadingFactor() { return spreadingFactor; }
    public String getCodingRate() { return codingRate; }
    public DeviceClass getDeviceClass() { return deviceClass; }
}
