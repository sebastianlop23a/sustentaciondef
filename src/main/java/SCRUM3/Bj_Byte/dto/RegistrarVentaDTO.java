package SCRUM3.Bj_Byte.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RegistrarVentaDTO {

    private String cliente;
    private String metodoPago;
    private LocalDateTime fechaVenta;

    // Lista de productos seleccionados en la venta
    private List<ProductoVentaDTO> productos;

    // Getters y Setters
    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public List<ProductoVentaDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoVentaDTO> productos) {
        this.productos = productos;
    }
}
