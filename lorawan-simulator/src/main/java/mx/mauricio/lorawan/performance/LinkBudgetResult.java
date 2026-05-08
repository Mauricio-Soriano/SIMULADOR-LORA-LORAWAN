package mx.mauricio.lorawan.performance;

public class LinkBudgetResult {
    private final String deviceId;
    private final String gatewayId;
    private final double distanceMeters;
    private final double pathLossDb;
    private final double txPowerDbm;
    private final double rxPowerDbm;
    private final double sensitivityDbm;
    private final double marginDb;
    private final boolean los;

    public LinkBudgetResult(String deviceId, String gatewayId, double distanceMeters, double pathLossDb,
                            double txPowerDbm, double rxPowerDbm, double sensitivityDbm,
                            double marginDb, boolean los) {
        this.deviceId = deviceId;
        this.gatewayId = gatewayId;
        this.distanceMeters = distanceMeters;
        this.pathLossDb = pathLossDb;
        this.txPowerDbm = txPowerDbm;
        this.rxPowerDbm = rxPowerDbm;
        this.sensitivityDbm = sensitivityDbm;
        this.marginDb = marginDb;
        this.los = los;
    }

    public String getDeviceId() { return deviceId; }
    public String getGatewayId() { return gatewayId; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getPathLossDb() { return pathLossDb; }
    public double getTxPowerDbm() { return txPowerDbm; }
    public double getRxPowerDbm() { return rxPowerDbm; }
    public double getSensitivityDbm() { return sensitivityDbm; }
    public double getMarginDb() { return marginDb; }
    public boolean isLos() { return los; }

    public String toCsvRow() {
        return String.join(",",
                deviceId,
                gatewayId,
                String.valueOf(distanceMeters),
                String.valueOf(pathLossDb),
                String.valueOf(txPowerDbm),
                String.valueOf(rxPowerDbm),
                String.valueOf(sensitivityDbm),
                String.valueOf(marginDb),
                String.valueOf(los));
    }
}
