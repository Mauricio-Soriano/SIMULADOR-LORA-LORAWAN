package mx.mauricio.lorawan.simulator.dto;

public class GatewayRequest {

    private String gatewayId;
    private double x;
    private double y;
    private int maxTxPowerDBm;
    private int udpPort;
    private int tcpPort;

    public GatewayRequest() {
        this.gatewayId = "gw-1";
        this.x = 100.0;
        this.y = 50.0;
        this.maxTxPowerDBm = 20;
        this.udpPort = 5000;
        this.tcpPort = 6000;
    }

    public GatewayRequest(String gatewayId, double x, double y, int maxTxPowerDBm, int udpPort, int tcpPort) {
        this.gatewayId = gatewayId;
        this.x = x;
        this.y = y;
        this.maxTxPowerDBm = maxTxPowerDBm;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
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

    public int getMaxTxPowerDBm() {
        return maxTxPowerDBm;
    }

    public void setMaxTxPowerDBm(int maxTxPowerDBm) {
        this.maxTxPowerDBm = maxTxPowerDBm;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }
}
