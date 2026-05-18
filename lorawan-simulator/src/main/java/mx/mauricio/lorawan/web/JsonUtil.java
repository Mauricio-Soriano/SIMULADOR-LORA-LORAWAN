package mx.mauricio.lorawan.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo a JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo JSON", e);
        }
    }
}
