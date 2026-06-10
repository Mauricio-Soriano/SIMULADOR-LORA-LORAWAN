package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

import mx.mauricio.lorawan.gateway.Gateway;

public class GatewayRegistry {

    private final Map<String, Gateway> gateways = new HashMap<>();

    public void register(Gateway gateway) {
        gateways.put(gateway.getGatewayId(), gateway);

        System.out.println(
            "[GatewayRegistry] Gateway registrado: "
            + gateway.getGatewayId()
        );
    }

    public Gateway get(String gatewayId) {
        return gateways.get(gatewayId);
    }

    public boolean contains(String gatewayId) {
        return gateways.containsKey(gatewayId);
    }

    public int size() {
        return gateways.size();
    }
}