package mx.mauricio.lorawan.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FuenteInformacion implements IFuenteVisualizable {

    private static final String[] FORMATOS_PERMITIDOS = {"txt", "csv"};

    private final Path rutaArchivo;
    private List<String> lineas = Collections.emptyList();

    public FuenteInformacion(String rutaArchivo) {
        this.rutaArchivo = Path.of(rutaArchivo);
        validarFormato();
    }

    private void validarFormato() {
        String nombre = rutaArchivo.getFileName().toString();
        int idx = nombre.lastIndexOf('.');
        String extension = (idx >= 0) ? nombre.substring(idx + 1).toLowerCase() : "";
        boolean permitido = false;

        for (String f : FORMATOS_PERMITIDOS) {
            if (f.equals(extension)) {
                permitido = true;
                break;
            }
        }

        if (!permitido) {
            throw new IllegalArgumentException("Formato no permitido: " + extension);
        }
    }

    @Override
    public void cargarInformacion() throws IOException {
        this.lineas = Files.readAllLines(rutaArchivo).stream()
                .skip(1) // salta encabezado CSV
                .map(this::normalizarLineaCsv)
                .filter(linea -> !linea.isBlank())
                .collect(Collectors.toList());

        imprimirPreview();
    }

    private String normalizarLineaCsv(String linea) {
        String[] cols = linea.split(",", -1);

        if (cols.length == 0) {
            return "";
        }

        // Si la fila tiene menos de 13 columnas, completar con vacíos
        String[] normalizada = new String[13];
        for (int i = 0; i < normalizada.length; i++) {
            if (i < cols.length) {
                normalizada[i] = cols[i].trim();
            } else {
                normalizada[i] = "";
            }
        }

        // Si una celda viene vacía, reemplazar por 0
        for (int i = 0; i < normalizada.length; i++) {
            if (normalizada[i] == null || normalizada[i].isBlank()) {
                normalizada[i] = "0";
            }
        }

        return String.join(",", normalizada);
    }

    @Override
    public void eliminarDatos() {
        this.lineas = Collections.emptyList();
    }

    @Override
    public void volverACargar() throws IOException {
        eliminarDatos();
        cargarInformacion();
    }

    public List<String> getLineas() {
        return lineas;
    }

    private void imprimirPreview() {
        System.out.println("== Preview de " + rutaArchivo + " ==");
        int limite = Math.min(IFuenteVisualizable.MAX_ELEMENTOS, lineas.size());
        for (int i = 0; i < limite; i++) {
            System.out.println((i + 1) + ": " + lineas.get(i));
        }
        if (lineas.size() > IFuenteVisualizable.MAX_ELEMENTOS) {
            System.out.println("... y " + (lineas.size() - IFuenteVisualizable.MAX_ELEMENTOS) + " líneas más");
        }
    }
}


