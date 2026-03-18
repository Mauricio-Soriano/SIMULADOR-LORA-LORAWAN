package mx.mauricio.lorawan.frame;

/**
 * Capa MAC LoRaWAN - MHDR + MACPayload + MIC
 * (Placeholder para implementación completa del MIC)
 */
public abstract class MacFrame extends Frame {
    protected byte mhdr = 0x40; // Unconfirmed Data Up
    protected byte[] mic = {0x00, 0x00, 0x00, 0x00}; // MIC placeholder

    public byte getMhdr() { return mhdr; }
    public byte[] getMic() { return mic; }
}

