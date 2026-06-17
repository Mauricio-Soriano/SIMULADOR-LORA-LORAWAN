package mx.mauricio.lorawan.network;

import java.util.Random;

public class PacketLossSimulator {

    private final Random random =
            new Random();

    private final double lossProbability;

    public PacketLossSimulator(
            double lossProbability) {

        this.lossProbability =
                lossProbability;
    }

    public boolean shouldDrop() {

        return random.nextDouble()
                < lossProbability;
    }
}