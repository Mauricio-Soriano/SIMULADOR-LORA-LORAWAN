package mx.mauricio.lorawan.network;

import java.util.Map;

public class UplinkProcessor {

    private final PayloadParser payloadParser =
            new PayloadParser();

    public UplinkContext process(String payload) {

        Map<String, String> fields =
                payloadParser.parse(payload);

        payloadParser.logFields(fields);

        if (!payloadParser.isValid(fields)) {
            return null;
        }

        String deviceId =
                fields.get("DEV");

        String decodedData =
                fields.get("DATA");

        String description =
                payloadParser.describeDecodedSource(
                        fields.get("FPORT"),
                        decodedData);

        String mhdr = fields.get("MHDR");
        boolean confirmed = "80".equals(mhdr);


        return new UplinkContext(
                fields,
                deviceId,
                decodedData,
                description,
                confirmed
        );
    }
}