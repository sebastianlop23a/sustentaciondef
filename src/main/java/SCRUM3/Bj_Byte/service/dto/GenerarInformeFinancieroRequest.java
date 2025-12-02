package SCRUM3.Bj_Byte.service.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Data Transfer Object que encapsula los datos de entrada para la generación
 * de informes financieros. Facilita la separación entre capas y validación
 * centralizada de parámetros.
 */
public class GenerarInformeFinancieroRequest {

    private String productoFiltro;
    private String empleadoFiltro;
    private LocalDate fechaFiltro;
    private String tipo;
    private Map<String, String> metadatos;

    public GenerarInformeFinancieroRequest() {
    }

    public GenerarInformeFinancieroRequest(String productoFiltro, String empleadoFiltro, LocalDate fechaFiltro) {
        this.productoFiltro = productoFiltro;
        this.empleadoFiltro = empleadoFiltro;
        this.fechaFiltro = fechaFiltro;
        this.tipo = "PDF";
    }

    public GenerarInformeFinancieroRequest(String productoFiltro, String empleadoFiltro, String fechaFiltroStr, String tipo, Map<String, String> metadatos) {
        this.productoFiltro = productoFiltro;
        this.empleadoFiltro = empleadoFiltro;
        this.tipo = tipo;
        this.metadatos = metadatos;
        if (fechaFiltroStr != null && !fechaFiltroStr.isEmpty()) {
            try {
                this.fechaFiltro = LocalDate.parse(fechaFiltroStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                this.fechaFiltro = null;
            }
        }
    }

    public String getProductoFiltro() {
        return productoFiltro;
    }

    public void setProductoFiltro(String productoFiltro) {
        this.productoFiltro = productoFiltro;
    }

    public String getEmpleadoFiltro() {
        return empleadoFiltro;
    }

    public void setEmpleadoFiltro(String empleadoFiltro) {
        this.empleadoFiltro = empleadoFiltro;
    }

    public LocalDate getFechaFiltro() {
        return fechaFiltro;
    }

    public void setFechaFiltro(LocalDate fechaFiltro) {
        this.fechaFiltro = fechaFiltro;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Map<String, String> getMetadatos() {
        return metadatos;
    }

    public void setMetadatos(Map<String, String> metadatos) {
        this.metadatos = metadatos;
    }

    public boolean esValido() {
        return true;
    }
}
