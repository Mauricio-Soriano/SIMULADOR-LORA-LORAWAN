package mx.mauricio.lorawan.simulator.dto;

import mx.mauricio.lorawan.config.LoRaConfig;

public class DeviceRequest {

    private String deviceId;
    private LoRaConfig config;
    private int fPort;
    private double x;
    private double y;
    private boolean enabled;

    public DeviceRequest() {
        this.enabled = true;
    }

    public DeviceRequest(String deviceId, LoRaConfig config, int fPort, double x, double y) {
        this.deviceId = deviceId;
        this.config = config;
        this.fPort = fPort;
        this.x = x;
        this.y = y;
        this.enabled = true;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LoRaConfig getConfig() {
        return config;
    }

    public void setConfig(LoRaConfig config) {
        this.config = config;
    }

    public int getFPort() {
        return fPort;
    }

    public void setFPort(int fPort) {
        this.fPort = fPort;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
