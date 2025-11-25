package SCRUM3.Bj_Byte.model;

import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;

@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    @Column(precision = 10, scale = 3)
    private BigDecimal precio;

    private String cvv; // Código o referencia interna del producto

    // Campo para marcar si el producto está exento de IVA
    @Column(nullable = false)
    private Boolean exento = false;

    // Estado del producto en el sistema
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany
    @JoinTable(
        name = "producto_proveedor",
        joinColumns = @JoinColumn(name = "producto_id"),
        inverseJoinColumns = @JoinColumn(name = "proveedor_id")
    )
    private Set<Proveedor> proveedores = new HashSet<>();


    /* =====================================================
                            GETTERS / SETTERS
       ===================================================== */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public Boolean getExento() {
        return exento;
    }

    public void setExento(Boolean exento) {
        this.exento = exento;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Set<Proveedor> getProveedores() {
        return proveedores;
    }

    public void setProveedores(Set<Proveedor> proveedores) {
        this.proveedores = proveedores;
    }
}
