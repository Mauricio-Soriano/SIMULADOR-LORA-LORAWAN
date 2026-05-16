package mx.mauricio.lorawan.simulator.dto;

import java.util.ArrayList;
import java.util.List;

import mx.mauricio.lorawan.config.LoRaConfig;

public class DeviceRequest {

    private String deviceId;
    private LoRaConfig config;
    private int fPort;
    private double x;
    private double y;
    private boolean enabled;
    private List<Integer> columnIndexes;

    public DeviceRequest() {
        this.enabled = true;
        this.columnIndexes = new ArrayList<>();
    }

    public DeviceRequest(String deviceId, LoRaConfig config, int fPort, double x, double y) {
        this.deviceId = deviceId;
        this.config = config;
        this.fPort = fPort;
        this.x = x;
        this.y = y;
        this.enabled = true;
        this.columnIndexes = new ArrayList<>();
    }

    public DeviceRequest(String deviceId, LoRaConfig config, int fPort, double x, double y, List<Integer> columnIndexes) {
        this.deviceId = deviceId;
        this.config = config;
        this.fPort = fPort;
        this.x = x;
        this.y = y;
        this.enabled = true;
        this.columnIndexes = (columnIndexes != null) ? new ArrayList<>(columnIndexes) : new ArrayList<>();
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

    public List<Integer> getColumnIndexes() {
        return columnIndexes;
    }

    public void setColumnIndexes(List<Integer> columnIndexes) {
        this.columnIndexes = (columnIndexes != null) ? new ArrayList<>(columnIndexes) : new ArrayList<>();
    }
}
