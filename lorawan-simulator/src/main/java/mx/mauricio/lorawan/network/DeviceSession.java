package mx.mauricio.lorawan.network;

public class DeviceSession {

    private final String deviceId;

    private int fCntUp;
    private int fCntDown;

    private String lastGatewayId;

    private double lastRssi;
    private double lastSnr;
    
    public DeviceSession(String deviceId) {
        this.deviceId = deviceId;
        this.fCntUp = 0;
        this.fCntDown = 0;
    }

    public int nextFCntDown() {
        return ++fCntDown;
    }

    public int getFCntDown() {
        return fCntDown;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getFCntUp() {
        return fCntUp;
    }

    public int incrementFCntUp() {
        return ++fCntUp;
    }


    public int incrementFCntDown() {
        return ++fCntDown;
    }

    public String getLastGatewayId() {
        return lastGatewayId;
    }

    public void setLastGatewayId(String lastGatewayId) {
        this.lastGatewayId = lastGatewayId;
    }

    public double getLastRssi() {
        return lastRssi;
    }

    public void setLastRssi(double lastRssi) {
        this.lastRssi = lastRssi;
    }

    public double getLastSnr() {
        return lastSnr;
    }

    public void setLastSnr(double lastSnr) {
        this.lastSnr = lastSnr;
    }
}