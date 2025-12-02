package SCRUM3.Bj_Byte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FECHA Y TOTAL
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now(); // Inicializado por defecto
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVenta;

    // INFORMACIÓN DEL EMPLEADO (Asumiendo que Empleado es una entidad)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    // Para guardar el nombre del empleado sin depender de la entidad (buena práctica para auditoría)
    private String nombreEmpleado;
    
    // INFORMACIÓN DEL CLIENTE
    
    // Cliente registrado (Opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente clienteRegistrado; // Suponiendo que existe la entidad Cliente

    // Nombre del cliente (manual o si clienteRegistrado es null)
    private String cliente; 

    // Datos opcionales del cliente no registrado
    private String clienteDocumento;
    private String clienteTelefono;
    private String clienteDireccion;
    
    // OTROS DETALLES
    
    @Column(nullable = false)
    private String metodoPago;

    // Detalles de los productos vendidos (Relación Fuerte: si se borra la Venta, se borran los Detalles)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    // Proveedores: Esta relación es un poco inusual para una Venta, 
    // si solo es para listar proveedores de los productos vendidos,
    // es mejor manejarla en el Controller/Service. Si realmente se guarda, mantenemos el Set.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "venta_proveedor",
            joinColumns = @JoinColumn(name = "venta_id"),
            inverseJoinColumns = @JoinColumn(name = "proveedor_id")
    )
    private Set<Proveedor> proveedores = new HashSet<>();

    // ============================================================
    // CONSTRUCTORES
    // ============================================================
    public Venta() {
        // Constructor vacío requerido por JPA
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public String getCliente() { 
        // Devuelve el nombre del cliente registrado si existe, sino el nombre libre
        return (clienteRegistrado != null && clienteRegistrado.getNombre() != null) 
               ? clienteRegistrado.getNombre() 
               : cliente; 
    }
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
    
    public Cliente getClienteRegistrado() { return clienteRegistrado; }
    public void setClienteRegistrado(Cliente clienteRegistrado) { this.clienteRegistrado = clienteRegistrado; }

    // ============================================================
    // MÉTODOS AUXILIARES (LÓGICA DE NEGOCIO)
    // ============================================================

    /** Agrega detalle a la lista y establece la relación bidireccional */
    public void agregarDetalle(VentaDetalle detalle) {
        if (detalles == null) detalles = new ArrayList<>();
        detalles.add(detalle);
        detalle.setVenta(this); // Importante para la relación bidireccional
    }

    /** Calcula el total sumando los detalles */
    @Transient // Indica a JPA que no es un campo de la base de datos
    public BigDecimal calcularTotal() {
        if (detalles == null || detalles.isEmpty()) return BigDecimal.ZERO;

        return detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ============================================================
    // MÉTODOS PARA THYMELEAF / FILTROS (Soluciona error anterior)
    // ============================================================

    /** * Genera una cadena con los nombres de los productos para ser usado en filtros de Thymeleaf/JS. 
     * Soluciona el error 'productosString' no encontrado.
     */
    @Transient 
    public String getProductosString() {
        if (detalles == null || detalles.isEmpty()) return "";
        
        return detalles.stream()
                .map(d -> d.getInventario() != null && d.getInventario().getProducto() != null 
                           ? d.getInventario().getProducto().getNombre()
                           : "Producto Desconocido")
                .collect(Collectors.joining(", "));
    }

    // ============================================================
    // MÉTODOS PARA COMPATIBILIDAD CON EL PDF (Marcados como Transient)
    // ============================================================

    /** Nombre del primer producto (si existe) */
    @Transient
    public String getPrimerNombreProducto() {
        if (detalles == null || detalles.isEmpty()) return "Producto";
        return detalles.get(0).getInventario().getProducto().getNombre();
    }

    /** Cantidad del primer producto */
    @Transient
    public Integer getPrimerCantidad() {
        if (detalles == null || detalles.isEmpty()) return 1;
        return detalles.get(0).getCantidad();
    }

    /** Inventario del primer detalle */
    @Transient
    public Inventario getPrimerInventario() {
        if (detalles == null || detalles.isEmpty()) return null;
        return detalles.get(0).getInventario();
    }
}
