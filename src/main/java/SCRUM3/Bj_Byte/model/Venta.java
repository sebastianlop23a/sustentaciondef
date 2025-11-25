package SCRUM3.Bj_Byte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    private BigDecimal totalVenta;

    // Nombre del cliente (manual o desde BD)
    private String cliente;

    // Datos opcionales del cliente
    private String clienteDocumento;
    private String clienteTelefono;
    private String clienteDireccion;

    private String metodoPago;

    // Para guardar el nombre del empleado sin depender de la entidad
    private String nombreEmpleado;

    @ManyToOne
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    // Detalles de los productos vendidos
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "venta_proveedor",
            joinColumns = @JoinColumn(name = "venta_id"),
            inverseJoinColumns = @JoinColumn(name = "proveedor_id")
    )
    private Set<Proveedor> proveedores = new HashSet<>();


    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public String getClienteDireccion() { return clienteDireccion; }
    public void setClienteDireccion(String clienteDireccion) { this.clienteDireccion = clienteDireccion; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public List<VentaDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<VentaDetalle> detalles) { this.detalles = detalles; }

    public Set<Proveedor> getProveedores() { return proveedores; }
    public void setProveedores(Set<Proveedor> proveedores) { this.proveedores = proveedores; }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    /** Agregar detalle a la lista */
    public void agregarDetalle(VentaDetalle detalle) {
        if (detalles == null) detalles = new ArrayList<>();
        detalles.add(detalle);
    }

    /** Calcula el total sumando los detalles */
    public BigDecimal calcularTotal() {
        if (detalles == null || detalles.isEmpty()) return BigDecimal.ZERO;

        return detalles.stream()
                .map(d -> d.getInventario().getProducto().getPrecio()
                        .multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ============================================================
    // MÉTODOS PARA COMPATIBILIDAD CON EL PDF
    // ============================================================

    /** Nombre del primer producto (si existe) */
    public String getPrimerNombreProducto() {
        if (detalles == null || detalles.isEmpty()) return "Producto";
        return detalles.get(0).getInventario().getProducto().getNombre();
    }

    /** Cantidad del primer producto */
    public Integer getPrimerCantidad() {
        if (detalles == null || detalles.isEmpty()) return 1;
        return detalles.get(0).getCantidad();
    }

    /** Inventario del primer detalle */
    public Inventario getPrimerInventario() {
        if (detalles == null || detalles.isEmpty()) return null;
        return detalles.get(0).getInventario();
    }
}
