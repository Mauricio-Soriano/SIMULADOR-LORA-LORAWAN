package mx.mauricio.lorawan.metrics;

import java.util.Collection;

import mx.mauricio.lorawan.network.DeviceSession;

public class MetricsReporter {

    public void printReport(
            Collection<DeviceSession> sessions) {

        System.out.println();
        System.out.println(
                "===== NETWORK METRICS =====");

        for (DeviceSession session : sessions) {

            System.out.println();

            System.out.println(
                    "Device: "
                    + session.getDeviceId());

            System.out.println("TxAttempts=" + session.getPacketsTransmitted());
                System.out.println("OriginalMessages=" + session.getOriginalMessages());
                System.out.println("Retransmissions=" + session.getRetransmissionAttempts());
                System.out.println("Rx=" + session.getPacketsReceived());
                System.out.println("Lost=" + session.getPacketsLost());
                System.out.println("LostByLinkBudget=" + session.getLinkBudgetLosses());
                System.out.println("LostByRandom=" + session.getRandomLosses());

                System.out.println(
                        "PDR="
                        + String.format("%.2f", session.getPdr())
                        + "%");

                System.out.println(
                        "DeliveryRate="
                        + String.format("%.2f", session.getDeliveryRate())
                        + "%");
        
        System.out.printf(
                "RSSI Avg=%.2f dBm%n",
                session.getAverageRssi());
        System.out.printf(
                "SNR Avg=%.2f dB%n",
                session.getAverageSnr()
                );

        System.out.printf(
                "Throughput=%.2f kbps%n",
                session.getThroughputKbps()
                );

        System.out.printf(
                "Latency Avg=%.2f ms%n",
                session.getAverageLatency()
        );
        }
    }
}