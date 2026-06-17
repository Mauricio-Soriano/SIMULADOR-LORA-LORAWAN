package mx.mauricio.lorawan.network;

public class DeviceSession {

    private final String deviceId;

    private int fCntUp;
    private int fCntDown;
    private int lastFCntUpReceived = -1;

    private int packetsReceived;
    private int packetsLost;
    private int lastFcnt = -1;

    private int packetsTransmitted;

    private int recommendedSf;


    private String lastGatewayId;

    private double lastRssi;
    private double lastSnr;

    private boolean ackRequired;
    
    public DeviceSession(String deviceId) {
        this.deviceId = deviceId;
        this.fCntUp = 0;
        this.fCntDown = 0;

        this.ackRequired = false;
        
    }

    public boolean isAckRequired() {
        return ackRequired;
    }

    public int getRecommendedSf() {
        return recommendedSf;
    }

    public void setRecommendedSf(
            int recommendedSf) {

        this.recommendedSf =
                recommendedSf;
    }

    

    public void setAckRequired(boolean ackRequired) {
        this.ackRequired = ackRequired;
    }

    public int getLastFCntUpReceived() {
        return lastFCntUpReceived;
    }

    public void setLastFCntUpReceived(int value) {
        this.lastFCntUpReceived = value;
    }

    public int nextFCntDown() {
        return ++fCntDown;
    }

    public void incrementPacketsReceived() {
        packetsReceived++;
    }

    public void incrementPacketsLost(int lost) {
        packetsLost += lost;
    }

    public void incrementPacketsTransmitted() {
        packetsTransmitted++;
    }

    public int getPacketsTransmitted() {
        return packetsTransmitted;
    }

    public int getLastFcnt() {
        return lastFcnt;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public int getPacketsLost() {
        return packetsLost;
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

    public double getPdr() {

        int total =
                packetsReceived
                + packetsLost;

        if (total == 0) {
            return 0;
        }

        return (packetsReceived * 100.0)
                / total;
    }


    public int incrementFCntDown() {
        return ++fCntDown;
    }

    public String getLastGatewayId() {
        return lastGatewayId;
    }

    public void setLastFcnt(int lastFcnt) {
        this.lastFcnt = lastFcnt;
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