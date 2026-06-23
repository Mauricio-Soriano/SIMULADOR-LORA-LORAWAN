package mx.mauricio.lorawan.network;

import java.util.ArrayList;
import java.util.List;

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

    private int currentSf = -1;

    private int originalMessages;
    private int retransmissionAttempts;
    private int linkBudgetLosses;
    private int randomLosses;
    private int lastAttemptedFcnt = -1;

    private final List<Double> rssiHistory =
        new ArrayList<>();

    private final List<Double> snrHistory = new ArrayList<>();

    private final List<Long> latencyHistory = new ArrayList<>();
    
    private long totalBytesReceived = 0;

    private long firstPacketTimestamp = 0;
    private long lastPacketTimestamp = 0;
    
    public DeviceSession(String deviceId) {
        this.deviceId = deviceId;
        this.fCntUp = 0;
        this.fCntDown = 0;

        this.ackRequired = false;
        
    }

    public void registerPacketTimestamp() {

        long now = System.currentTimeMillis();

        if(firstPacketTimestamp == 0) {
            firstPacketTimestamp = now;
        }

        lastPacketTimestamp = now;
    }

    public double getThroughputKbps() {

        if(firstPacketTimestamp == 0
                || lastPacketTimestamp == 0) {
            return 0.0;
        }

        long durationMs =
                lastPacketTimestamp
                - firstPacketTimestamp;

        if(durationMs <= 0) {
            return 0.0;
        }

        double seconds =
                durationMs / 1000.0;

        double bits =
                totalBytesReceived * 8.0;

        return (bits / seconds) / 1000.0;
    }

    public int getCurrentSf() {
        return currentSf;
    }

    public void setCurrentSf(int currentSf) {
        this.currentSf = currentSf;
    }



    public void addReceivedBytes(long bytes) {
        totalBytesReceived += bytes;
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    public void addRssi(double rssi) {

        rssiHistory.add(rssi);
    }

    public void addSnr(double snr) {
        snrHistory.add(snr);
    }

    public void addLatency(long latencyMs)
    {
        latencyHistory.add(latencyMs);
    }

    public double getAverageSnr() {

        if (snrHistory.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;

        for (double snr : snrHistory) {
            sum += snr;
        }

        return sum / snrHistory.size();
    }

    public double getAverageLatency()
    {
        if(latencyHistory.isEmpty())
        {
            return 0.0;
        }

        long sum = 0;

        for(long latency : latencyHistory)
        {
            sum += latency;
        }

        return (double) sum / latencyHistory.size();
    }

    public double getAverageRssi() {

        if (rssiHistory.isEmpty()) {
            return 0;
        }

        double sum = 0;

        for (double rssi : rssiHistory) {
            sum += rssi;
        }

        return sum / rssiHistory.size();
    }

    public void registerTransmissionAttempt(int fcnt) {

        packetsTransmitted++;

        if (lastAttemptedFcnt == fcnt) {

            retransmissionAttempts++;

        } else {

            originalMessages++;
            lastAttemptedFcnt = fcnt;
        }
    }

    public void incrementLinkBudgetLosses(int count) {

        linkBudgetLosses += count;
        packetsLost += count;
    }

    public void incrementRandomLosses(int count) {

        randomLosses += count;
        packetsLost += count;
    }

    public int getOriginalMessages() {
        return originalMessages;
    }

    public int getRetransmissionAttempts() {
        return retransmissionAttempts;
    }

    public int getLinkBudgetLosses() {
        return linkBudgetLosses;
    }

    public int getRandomLosses() {
        return randomLosses;
    }

    public double getDeliveryRate() {

        if (originalMessages == 0) {
            return 0.0;
        }

        return ((double) packetsReceived / originalMessages) * 100.0;
    }

    public int getRssiSamples() {

        return rssiHistory.size();
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