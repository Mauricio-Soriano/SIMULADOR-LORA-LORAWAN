package mx.mauricio.lorawan.network.policy;

public class DownlinkDecision {

    private final boolean shouldSend;
    private final String command;
    private final String fPort;

    private DownlinkDecision(boolean shouldSend, String command, String fPort) {
        this.shouldSend = shouldSend;
        this.command = command;
        this.fPort = fPort;
    }

    public static DownlinkDecision none() {
        return new DownlinkDecision(false, null, null);
    }

    public static DownlinkDecision send(String command, String fPort) {
        return new DownlinkDecision(true, command, fPort);
    }

    public boolean shouldSend() {
        return shouldSend;
    }

    public String getCommand() {
        return command;
    }

    public String getFPort() {
        return fPort;
    }
}