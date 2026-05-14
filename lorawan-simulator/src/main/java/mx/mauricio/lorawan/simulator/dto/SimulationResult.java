package mx.mauricio.lorawan.simulator.dto;

public class SimulationResult {

    private boolean success;
    private int rowsProcessed;
    private int rowsSkipped;
    private int devicesConfigured;
    private String message;

    public SimulationResult() {
    }

    public SimulationResult(boolean success, int rowsProcessed, int rowsSkipped, int devicesConfigured, String message) {
        this.success = success;
        this.rowsProcessed = rowsProcessed;
        this.rowsSkipped = rowsSkipped;
        this.devicesConfigured = devicesConfigured;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(int rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public int getRowsSkipped() {
        return rowsSkipped;
    }

    public void setRowsSkipped(int rowsSkipped) {
        this.rowsSkipped = rowsSkipped;
    }

    public int getDevicesConfigured() {
        return devicesConfigured;
    }

    public void setDevicesConfigured(int devicesConfigured) {
        this.devicesConfigured = devicesConfigured;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
