package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.model.*;
import SCRUM3.Bj_Byte.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // 👈 Importación clave
import org.springframework.data.domain.Pageable; // 👈 Importación clave
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    // --- Repositorio Necesario para Filtros ---
    @Autowired
    private ProductoRepository productoRepository;


    // =========================================================
    // MÉTODOS DE BÚSQUEDA Y FILTRADO (CON PAGINACIÓN) 🔎
    // =========================================================

    /**
     * Busca ventas aplicando filtros opcionales de producto, empleado y fecha, 
     * y maneja la paginación.
     * * @param producto Nombre del producto a filtrar (o null/vacío)
     * @param empleado Nombre del empleado a filtrar (o null/vacío)
     * @param fecha Fecha en formato 'yyyy-MM-dd' a filtrar (o null/vacío)
     * @param pageable Objeto Pageable para configurar la paginación y ordenamiento
     * @return Una página de resultados de ventas (Page<Venta>)
     */
    @Transactional(readOnly = true)
    public Page<Venta> buscarVentasConPaginacion(
            String producto,
            String empleado,
            String fecha,
            Pageable pageable) {

        return ventaRepository.buscarVentasConFiltrosPaginadas(producto, empleado, fecha, pageable);
    }


    // =========================================================
    // MÉTODOS DE FILTRADO PARA EL FRONT-END (SELECTS) 🔎
    // =========================================================

    /**
     * Obtiene una lista de todos los nombres de productos distintos para llenar el filtro.
     * 🚨 Asume que ProductoRepository tiene el método: List<String> findDistinctNames();
     */
    @Transactional(readOnly = true)
    public List<String> obtenerNombresProductosUnicos() {
        return productoRepository.findDistinctNames(); 
    }

    /**
     * Obtiene una lista de todos los nombres de empleados distintos para llenar el filtro.
     */
    @Transactional(readOnly = true)
    public List<String> obtenerNombresEmpleadosUnicos() {
        return empleadoRepository.findAll().stream()
                .map(Empleado::getNombre)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista de todas las fechas de venta (solo la parte de la fecha) 
     * en formato 'yyyy-MM-dd' para llenar el filtro.
     * 🚨 Se basa en el método findDistinctDates() de VentaRepository.
     */
    @Transactional(readOnly = true)
    public List<String> obtenerFechasUnicasFormatoYYYYMMDD() {
        return ventaRepository.findDistinctDates(); 
    }


    // =========================================================
    // MÉTODOS DE LÓGICA DE NEGOCIO (REGISTRAR VENTA) 🛒
    // =========================================================
    
    /**
     * Registra una venta con varios productos.
     * NOTA: Este método es redundante ya que el VentaController maneja el registro
     * con @Transactional, pero lo mantengo por completitud del servicio.
     */
    @Transactional
    public void registrarVenta(List<VentaDetalle> detalles, String cliente, String metodoPago, Long empleadoId) {

        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("No se puede registrar una venta sin productos.");
        }

        // Obtener empleado
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));

        // Crear venta principal
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setCliente(cliente);
        venta.setMetodoPago(metodoPago);
        venta.setEmpleado(empleado);
        venta.setNombreEmpleado(empleado.getNombre());

        BigDecimal totalGeneral = BigDecimal.ZERO;

        // Procesar cada detalle de venta
        for (VentaDetalle detalle : detalles) {
            Inventario inventario = inventarioRepository.findById(detalle.getInventario().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventario no encontrado."));

            int cantidadVendida = detalle.getCantidad();

            if (inventario.getCantidad() < cantidadVendida) {
                throw new IllegalArgumentException("No hay suficiente stock para el producto: "
                        + inventario.getProducto().getNombre());
            }

            // Descontar inventario
            inventario.setCantidad(inventario.getCantidad() - cantidadVendida);
            inventarioRepository.save(inventario);

            // Calcular subtotal (Este cálculo es muy básico y no incluye IVA. 
            // El Controller tiene una lógica de cálculo más robusta, usar esta como referencia)
            BigDecimal precioUnitario = inventario.getProducto().getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidadVendida));


            // Crear y configurar el nuevo detalle
            VentaDetalle nuevoDetalle = new VentaDetalle();
            nuevoDetalle.setVenta(venta);
            nuevoDetalle.setInventario(inventario);
            nuevoDetalle.setCantidad(cantidadVendida);
            
            // Asumo que Venta tiene un método para agregar detalles
            // y que los detalles tienen campos para subtotal, iva y totalLinea
            // Si el objeto VentaDetalle está usando el subtotal/iva/totalLinea del Controller, 
            // este método necesitaría esos campos en los detalles que se le pasan.
            
            venta.agregarDetalle(nuevoDetalle);
            totalGeneral = totalGeneral.add(subtotal); // Sumar el subtotal (simplificado)
        }

        // Guardar venta con total calculado
        venta.setTotalVenta(totalGeneral);
        ventaRepository.save(venta);
    }
}