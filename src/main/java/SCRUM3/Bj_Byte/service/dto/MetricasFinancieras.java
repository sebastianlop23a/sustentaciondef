package SCRUM3.Bj_Byte.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object que encapsula las métricas calculadas del informe financiero.
 * Facilita el paso de datos entre capas sin exponer la lógica interna.
 */
public class MetricasFinancieras {

    private BigDecimal totalVentas;
    private Integer totalArticulos;
    private Integer totalTransacciones;
    private BigDecimal promedioVenta;
    private LocalDateTime fechaGeneracion;

    public MetricasFinancieras(BigDecimal totalVentas, Integer totalArticulos,
                               Integer totalTransacciones, BigDecimal promedioVenta,
                               LocalDateTime fechaGeneracion) {
        this.totalVentas = totalVentas;
        this.totalArticulos = totalArticulos;
        this.totalTransacciones = totalTransacciones;
        this.promedioVenta = promedioVenta;
        this.fechaGeneracion = fechaGeneracion;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public Integer getTotalArticulos() {
        return totalArticulos;
    }

    public Integer getTotalTransacciones() {
        return totalTransacciones;
    }

    public BigDecimal getPromedioVenta() {
        return promedioVenta;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }
}
