package mx.mauricio.lorawan.performance;

import java.util.List;

public class PerformanceVisualizer {
    public String toCsv(List<PerformanceMetric> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestampMillis,deviceId,gatewayId,distanceMeters,pathLossDb,txPowerDbm,rxPowerDbm,sensitivityDbm,marginDb,los\n");

        for (PerformanceMetric metric : metrics) {
            LinkBudgetResult r = metric.getLinkBudgetResult();
            sb.append(metric.getTimestampMillis()).append(',')
              .append(r.getDeviceId()).append(',')
              .append(r.getGatewayId()).append(',')
              .append(r.getDistanceMeters()).append(',')
              .append(r.getPathLossDb()).append(',')
              .append(r.getTxPowerDbm()).append(',')
              .append(r.getRxPowerDbm()).append(',')
              .append(r.getSensitivityDbm()).append(',')
              .append(r.getMarginDb()).append(',')
              .append(r.isLos()).append('\n');
        }
        return sb.toString();
    }
}
