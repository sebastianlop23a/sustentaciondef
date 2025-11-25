package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.model.*;
import SCRUM3.Bj_Byte.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

  

    /**
     * Registra una venta con varios productos.
     *
     * @param detalles Lista de objetos VentaDetalle (producto + cantidad)
     * @param cliente Nombre del cliente
     * @param metodoPago Método de pago
     * @param empleadoId ID del empleado que realiza la venta
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

            // Calcular subtotal
            BigDecimal precioUnitario = inventario.getProducto().getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidadVendida));

            // Guardar detalle (usamos el inventario/producto para obtener nombre y precio cuando haga falta)
            VentaDetalle nuevoDetalle = new VentaDetalle();
            nuevoDetalle.setVenta(venta);
            nuevoDetalle.setInventario(inventario);
            nuevoDetalle.setCantidad(cantidadVendida);

            venta.agregarDetalle(nuevoDetalle);
            totalGeneral = totalGeneral.add(subtotal);
        }

        // Guardar venta con total calculado
        venta.setTotalVenta(totalGeneral);
        ventaRepository.save(venta);
    }
}
