package mx.mauricio.lorawan.simulator.dto;

import java.util.ArrayList;
import java.util.List;
import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters;

public class SimulationRequest {

    private String inputFile;
    private int rowsToProcess;
    private int sendIntervalMs;
    private GatewayRequest gateway;
    private List<DeviceRequest> devices;
    private Boolean adrEnabled = true;

    private Boolean randomLossEnabled = false;

    private Double randomLossProbability = 0.0;
    private Cost231LinkBudgetParameters linkBudgetParameters;

    public SimulationRequest() {
        this.inputFile = "data/t5.csv";
        this.rowsToProcess = 100;
        this.sendIntervalMs = 150;
        this.gateway = new GatewayRequest();
        this.devices = new ArrayList<>();
    }

    public Boolean getAdrEnabled() {
        return adrEnabled;
    }

    public void setAdrEnabled(Boolean adrEnabled) {
        this.adrEnabled = adrEnabled;
    }

    public Cost231LinkBudgetParameters getLinkBudgetParameters() {
        return linkBudgetParameters;
    }

    public void setLinkBudgetParameters(
            Cost231LinkBudgetParameters linkBudgetParameters) {

        this.linkBudgetParameters =
                linkBudgetParameters;
    }

    public Boolean getRandomLossEnabled() {
        return randomLossEnabled;
    }

    public void setRandomLossEnabled(Boolean randomLossEnabled) {
        this.randomLossEnabled = randomLossEnabled;
    }

    public Double getRandomLossProbability() {
        return randomLossProbability;
    }

    public void setRandomLossProbability(Double randomLossProbability) {
        this.randomLossProbability = randomLossProbability;
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
