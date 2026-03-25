package mx.mauricio.lorawan.LoRaWAN;

public class MacHeader {
    private String mhdr;

    public MacHeader(String mhdr) {
        this.mhdr = mhdr;
    }

    @Override
    public String toString() {
        return "MHDR=" + mhdr;
    }
}

