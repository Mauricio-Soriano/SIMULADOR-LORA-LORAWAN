package mx.mauricio.lorawan.frame;

public class DownlinkFrame {
    private final String mhdr = "60"; // MHDR para Downlink
    private final String devAddr;
    private final String fCnt;
    private final String fPort;
    private final String frmPayload;
    private final String mic = "0000";
    private boolean ack;

    public DownlinkFrame(
            String devAddr,
            String fCnt,
            String fPort,
            String frmPayload,
            boolean ack) {

        this.devAddr = devAddr;
        this.fCnt = fCnt;
        this.fPort = fPort;
        this.frmPayload = frmPayload;
        this.ack = ack;
    }

    // Método para convertir el objeto a la cadena que enviará el socket
    public String toPayloadString() {

        return "MHDR=" + mhdr +
            "|ACK=" + ack +
            "|DEV=" + devAddr +
            "|FCNT=" + fCnt +
            "|TXTIME=" + System.currentTimeMillis() +
            "|FPORT=" + fPort +
            "|DATA=" + frmPayload +
            "|MIC=" + mic;
            
    }

    public boolean isAck() {
        return ack;
    }

    public String getDevAddr() { return devAddr; }
    public String getFrmPayload() { return frmPayload; }
}