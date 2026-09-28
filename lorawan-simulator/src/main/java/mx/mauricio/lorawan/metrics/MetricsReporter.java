package mx.mauricio.lorawan.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.mauricio.lorawan.network.DeviceSession;
import mx.mauricio.lorawan.performance.LinkBudgetResult;
import mx.mauricio.lorawan.performance.PerformanceMetric;
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

            System.out.println(
                    "TxAttempts="
                    + session.getPacketsTransmitted());

            System.out.println(
                    "OriginalMessages="
                    + session.getOriginalMessages());

            System.out.println(
                    "Retransmissions="
                    + session.getRetransmissionAttempts());

            System.out.println(
                    "Rx="
                    + session.getPacketsReceived());

            System.out.println(
                    "Lost="
                    + session.getPacketsLost());

            System.out.println(
                    "LostByLinkBudget="
                    + session.getLinkBudgetLosses());

            System.out.println(
                    "LostByRandom="
                    + session.getRandomLosses());

            System.out.println(
                    "PDR="
                    + String.format(
                            "%.2f",
                            session.getPdr())
                    + "%");

            System.out.println(
                    "DeliveryRate="
                    + String.format(
                            "%.2f",
                            session.getDeliveryRate())
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
                    "RxPower Avg=%.2f dBm%n",
                    session.getAverageRssi());

            System.out.printf(
                    "LinkMargin Avg=%.2f dB%n",
                    session.getAverageLinkMargin());

            System.out.printf(
                    "Throughput=%.2f kbps%n",
                    session.getThroughputKbps());

            System.out.printf(
                    "Latency Avg=%.2f ms%n",
                    session.getAverageLatency());
        }
    }

    /**
     * Compatibilidad con el contrato usado en la Fase 6.4.1.
     * No incluye métricas de propagación si no se proporciona el historial
     * de PerformanceMetric.
     */
    public List<SimulationResult.DeviceMetric> buildMetrics(
            Collection<DeviceSession> sessions) {

        return buildMetrics(
                sessions,
                List.of());
    }

    /**
     * Construye la respuesta completa para la Fase 6.4.2.
     *
     * Las métricas de red provienen de DeviceSession.
     * Las métricas radio provienen de PerformanceMetric -> LinkBudgetResult.
     *
     * El frontend no recalcula COST231.
     */
    public List<SimulationResult.DeviceMetric> buildMetrics(
            Collection<DeviceSession> sessions,
            Collection<PerformanceMetric> performanceMetrics) {

        List<SimulationResult.DeviceMetric> metrics =
                new ArrayList<>();

        Map<String, RadioAccumulator> radioByDevice =
                buildRadioAccumulators(
                        performanceMetrics);

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
                    session.getAverageLinkMargin();

            metric.throughputKbps =
                    session.getThroughputKbps();

            metric.latencyAvgMs =
                    session.getAverageLatency();

            RadioAccumulator radio =
                    radioByDevice.get(
                            session.getDeviceId());

            if (radio != null
                    && radio.samples > 0) {

                metric.radioSamples =
                        radio.samples;

                metric.distanceMeters =
                        radio.distanceSum
                        / radio.samples;

                metric.pathLossDb =
                        radio.pathLossSum
                        / radio.samples;

                metric.rxPowerDbm =
                        radio.rxPowerSum
                        / radio.samples;

                metric.radioLinkMarginAvgDb =
                        radio.linkMarginSum
                        / radio.samples;

                metric.los =
                        radio.los;
            }

            metrics.add(metric);
        }

        return metrics;
    }

    private Map<String, RadioAccumulator>
            buildRadioAccumulators(
                    Collection<PerformanceMetric> performanceMetrics) {

        Map<String, RadioAccumulator> result =
                new HashMap<>();

        if (performanceMetrics == null) {
            return result;
        }

        for (PerformanceMetric metric : performanceMetrics) {

            if (metric == null
                    || metric.getLinkBudgetResult() == null) {

                continue;
            }

            LinkBudgetResult linkBudget =
                    metric.getLinkBudgetResult();

            String deviceId =
                    linkBudget.getDeviceId();

            if (deviceId == null) {
                continue;
            }

            RadioAccumulator accumulator =
                    result.computeIfAbsent(
                            deviceId,
                            key -> new RadioAccumulator());

            accumulator.samples++;
            accumulator.distanceSum +=
                    linkBudget.getDistanceMeters();

            accumulator.pathLossSum +=
                    linkBudget.getPathLossDb();

            accumulator.rxPowerSum +=
                    linkBudget.getRxPowerDbm();

            accumulator.linkMarginSum +=
                    linkBudget.getMarginDb();

            accumulator.los =
                    linkBudget.isLos();
        }

        return result;
    }

    private static class RadioAccumulator {

        private int samples;

        private double distanceSum;
        private double pathLossSum;
        private double rxPowerSum;
        private double linkMarginSum;

        private boolean los;
    }
}
