package mx.mauricio.lorawan.simulator.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationRequest {

    private String inputFile;
    private int rowsToProcess;
    private int sendIntervalMs;
    private GatewayRequest gateway;
    private List<DeviceRequest> devices;

    public SimulationRequest() {
        this.inputFile = "data/t5.csv";
        this.rowsToProcess = 100;
        this.sendIntervalMs = 150;
        this.gateway = new GatewayRequest();
        this.devices = new ArrayList<>();
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public int getRowsToProcess() {
        return rowsToProcess;
    }

    public void setRowsToProcess(int rowsToProcess) {
        this.rowsToProcess = rowsToProcess;
    }

    public int getSendIntervalMs() {
        return sendIntervalMs;
    }

    public void setSendIntervalMs(int sendIntervalMs) {
        this.sendIntervalMs = sendIntervalMs;
    }

    public GatewayRequest getGateway() {
        return gateway;
    }

    public void setGateway(GatewayRequest gateway) {
        this.gateway = gateway;
    }

    public List<DeviceRequest> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceRequest> devices) {
        this.devices = devices;
    }

    public void addDevice(DeviceRequest device) {
        this.devices.add(device);
    }
}
