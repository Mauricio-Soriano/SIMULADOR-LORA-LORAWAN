package mx.mauricio.lorawan.network.policy;

public class DownlinkDecision {

    private final boolean shouldSend;
    private final String command;
    private final String fPort;
    private final String adrCommand;

    private DownlinkDecision(
            boolean shouldSend,
            String command,
            String fPort,
            String adrCommand) {

        this.shouldSend = shouldSend;
        this.command = command;
        this.fPort = fPort;
        this.adrCommand = adrCommand;
    }

    public static DownlinkDecision none() {
        return new DownlinkDecision(
            false,
            null,
            null,
            null);
    }

    public static DownlinkDecision send(String command, String fPort) {
        return new DownlinkDecision(
            true,
            command,
            fPort,
            null);
    }

    public String getAdrCommand() {
        return adrCommand;
    }

    public static DownlinkDecision adr(
            String adrCommand) {

        return new DownlinkDecision(
                true,
                null,
                "99",
                adrCommand);
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