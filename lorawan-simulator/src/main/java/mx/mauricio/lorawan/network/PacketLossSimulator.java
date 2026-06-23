package mx.mauricio.lorawan.network;

import java.util.Random;

public class PacketLossSimulator {

    private final Random random =
            new Random();

    private boolean enabled;

    private double lossProbability;

    public PacketLossSimulator() {
        this(false, 0.0);
    }

    public PacketLossSimulator(double lossProbability) {
        this(true, lossProbability);
    }

    public PacketLossSimulator(
            boolean enabled,
            double lossProbability) {

        this.enabled = enabled;
        setLossProbability(lossProbability);
    }

    public boolean shouldDrop() {

        if (!enabled) {
            return false;
        }

        if (lossProbability <= 0.0) {
            return false;
        }

        return random.nextDouble() < lossProbability;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getLossProbability() {
        return lossProbability;
    }

    public void setLossProbability(double lossProbability) {

        if (lossProbability < 0.0 || lossProbability > 1.0) {

            throw new IllegalArgumentException(
                    "La probabilidad de pérdida debe estar entre 0.0 y 1.0");
        }

        this.lossProbability =
                lossProbability;
    }
}