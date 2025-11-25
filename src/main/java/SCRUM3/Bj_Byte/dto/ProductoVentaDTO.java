package SCRUM3.Bj_Byte.dto;

public class ProductoVentaDTO {

    private Long inventarioId;
    private int cantidad;

    // Getters y Setters
    public Long getInventarioId() {
        return inventarioId;
    }

    public void setInventarioId(Long inventarioId) {
        this.inventarioId = inventarioId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
