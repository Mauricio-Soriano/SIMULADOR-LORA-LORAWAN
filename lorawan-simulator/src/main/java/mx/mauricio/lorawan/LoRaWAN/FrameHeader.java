package mx.mauricio.lorawan.LoRaWAN;

public class FrameHeader {
    private String devAddr;
    private String fcnt;

    public FrameHeader(String devAddr, int fcnt) {
        this.devAddr = devAddr;
        this.fcnt = String.format("%04X", fcnt);
    }

    @Override
    public String toString() {
        return String.format("FHDR[DevAddr=%s,FCnt=%s]", devAddr, fcnt);
        // ← SÓLO esto. BORRA cualquier referencia a device/appPayload
    }
}
