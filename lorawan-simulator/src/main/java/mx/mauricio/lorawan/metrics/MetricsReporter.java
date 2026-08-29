package mx.mauricio.lorawan.metrics;

import java.util.ArrayList;
import java.util.List;

import java.util.Collection;

import mx.mauricio.lorawan.network.DeviceSession;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;

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

                System.out.println(
                        "ConfirmedMessages="
                        + session.getConfirmedMessages());

                System.out.println(
                        "ACKGenerated="
                        + session.getAckGenerated());

                System.out.println(
                        "ACKReceived="
                        + session.getAckReceived());

                System.out.println(
                        "ACKLost="
                        + session.getAckLost());

                System.out.println(
                        "ConfirmedSuccessRate="
                        + String.format(
                                "%.2f",
                                session.getConfirmedSuccessRate())
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

    public List<SimulationResult.DeviceMetric> buildMetrics(
                Collection<DeviceSession> sessions) {

        List<SimulationResult.DeviceMetric> metrics =
                new ArrayList<>();

        for (DeviceSession session : sessions) {

                SimulationResult.DeviceMetric metric =
                        new SimulationResult.DeviceMetric();

                metric.deviceId =
                        session.getDeviceId();

                metric.txAttempts =
                        session.getPacketsTransmitted();

                metric.originalMessages =
                        session.getOriginalMessages();

                metric.retransmissions =
                        session.getRetransmissionAttempts();

                metric.rx =
                        session.getPacketsReceived();

                metric.lost =
                        session.getPacketsLost();

                metric.lostByLinkBudget =
                        session.getLinkBudgetLosses();

                metric.lostByRandom =
                        session.getRandomLosses();

                metric.pdr =
                        session.getPdr();

                metric.deliveryRate =
                        session.getDeliveryRate();

                metric.confirmedMessages =
                        session.getConfirmedMessages();

                metric.ackGenerated =
                        session.getAckGenerated();

                metric.ackReceived =
                        session.getAckReceived();

                metric.ackLost =
                        session.getAckLost();

                metric.confirmedSuccessRate =
                        session.getConfirmedSuccessRate();

                metric.rssiAvg =
                        session.getAverageRssi();

                metric.linkMarginAvg =
                        session.getAverageSnr();

                metric.throughputKbps =
                        session.getThroughputKbps();

                metric.latencyAvgMs =
                        session.getAverageLatency();

                metrics.add(metric);
        }

        return metrics;
        }
}