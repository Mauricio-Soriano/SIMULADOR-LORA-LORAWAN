package mx.mauricio.lorawan.frame;

/**
 * Capa Frame LoRaWAN - Campos comunes a todas las tramas
 */
public abstract class Frame {
    protected byte[] devAddress = {(byte)0x01, (byte)0x23, (byte)0x45, (byte)0x67};
    protected int frameCounter = 1;

    public byte[] getDevAddress() { return devAddress; }
    public int getFrameCounter() { return frameCounter; }
    
    public abstract String toHexString();
}
