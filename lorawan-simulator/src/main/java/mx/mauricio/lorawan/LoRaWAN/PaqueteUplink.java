package mx.mauricio.lorawan.LoRaWAN;

public class PaqueteUplink {
    private MacHeader macHeader;
    private String macPayload;      // Datos de aplicación
    private String mic;             // MIC (placeholder por ahora)
    private FrameHeader frameHeader;
    private int fPort;
    
    public PaqueteUplink(String devAddr, int fcnt, String payload, int fport) {
        this.macHeader = new MacHeader("40");  // MHDR=40: Unconfirmed Data Up
        this.macPayload = payload;
        this.mic = "0000";  // MIC placeholder
        this.frameHeader = new FrameHeader(devAddr, fcnt);
        this.fPort = fport;
    }
    
    @Override
    public String toString() {
        return String.format("[PaqueteUplink] %s %s FPort=%d FRMPayload='%s' MIC=%s", 
            macHeader, frameHeader, fPort, macPayload, mic);
    }
    
    // Getters para usar después si necesitas
    public String getMacPayload() { return macPayload; }
    public int getfPort() { return fPort; }
}
