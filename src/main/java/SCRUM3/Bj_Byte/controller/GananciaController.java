package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.model.VentaDetalle;
import SCRUM3.Bj_Byte.repository.VentaDetalleRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/ganancias")
public class GananciaController {

    @Autowired
    private VentaDetalleRepository ventaDetalleRepository; 

    private Empleado getEmpleadoLogueado(HttpSession session) {
        return (Empleado) session.getAttribute("empleadoLogueado");
    }

    /**
     * Muestra la vista de Ganancias Totales.
     * Ahora acepta parámetros de fecha para filtrar.
     */
    @GetMapping
    public String mostrarGanancias(
            HttpSession session, 
            Model model,
            @RequestParam(required = false) String fechaInicio, // Fecha de inicio del filtro
            @RequestParam(required = false) String fechaFin      // Fecha de fin del filtro
        ) {
        
        Empleado empleado = getEmpleadoLogueado(session);

        if (empleado == null) {
            return "redirect:/empleados/login";
        }
        
        // Asumiendo que solo los administradores (Rol ID 1) pueden ver esta vista
        if (empleado.getRolId() != 1) {
             model.addAttribute("alerta", "Acceso denegado. Solo administradores pueden ver las ganancias.");
             return "home";
        }

        // 1. Determinar el rango de fechas para el cálculo
        LocalDateTime inicio = null;
        LocalDateTime fin = null;
        String tituloGanancia = "HISTÓRICA TOTAL";

        // Si se proporciona fecha de inicio, la usamos
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            try {
                // Parsea la fecha (formato yyyy-MM-dd esperado del input type="date")
                LocalDate localDateInicio = LocalDate.parse(fechaInicio, DateTimeFormatter.ISO_DATE);
                inicio = localDateInicio.atStartOfDay();
                
                // Si también se proporciona fecha de fin
                if (fechaFin != null && !fechaFin.isEmpty()) {
                    LocalDate localDateFin = LocalDate.parse(fechaFin, DateTimeFormatter.ISO_DATE);
                    // El fin incluye todo el día hasta un segundo antes del día siguiente
                    fin = localDateFin.plusDays(1).atStartOfDay(); 
                    
                    tituloGanancia = "NETA ENTRE " + fechaInicio + " Y " + fechaFin;
                } else {
                    // Si solo hay fecha de inicio, asumimos que es para un solo día
                    fin = localDateInicio.plusDays(1).atStartOfDay();
                    tituloGanancia = "NETA DEL DÍA " + fechaInicio;
                }

            } catch (Exception e) {
                // Manejar error de formato si es necesario, por ahora solo ignora el filtro
                model.addAttribute("error", "Formato de fecha inválido. Mostrando ganancia histórica.");
            }
        }
        
        // 2. Calcular la ganancia con o sin filtro
        BigDecimal gananciaTotal = calcularGanancia(inicio, fin);

        // 3. Pasar los datos a la vista
        model.addAttribute("gananciaTotal", gananciaTotal);
        model.addAttribute("empleado", empleado);
        model.addAttribute("fechaInicioFiltro", fechaInicio);
        model.addAttribute("fechaFinFiltro", fechaFin);
        model.addAttribute("tituloGanancia", tituloGanancia);

        // 4. Retornar la vista
        return "ganancias/ganancias";
    }
    
    /**
     * Lógica para calcular la ganancia.
     * Si 'inicio' o 'fin' son null, calcula la ganancia histórica total.
     */
    private BigDecimal calcularGanancia(LocalDateTime inicio, LocalDateTime fin) {
        List<VentaDetalle> detalles;
        
        if (inicio != null && fin != null) {
            // Encuentra los detalles de venta en el rango de fechas
            // NECESITAS UNA QUERY EN VentaDetalleRepository para filtrar por fecha
            // Si tu VentaDetalle no tiene campo de fecha, tienes que hacer un Join 
            // con la tabla Venta, que SÍ tiene la fecha de creación (Venta.fechaCreacion)
            
            // Supondremos que has añadido un método:
            // List<VentaDetalle> findDetallesByVentaFechaCreacionBetween(LocalDateTime start, LocalDateTime end);
            detalles = ventaDetalleRepository.findDetallesInDateRange(inicio, fin);
        } else {
            // Ganancia histórica total
            detalles = ventaDetalleRepository.findAll();
        }

        BigDecimal gananciaTotal = BigDecimal.ZERO;

        for (VentaDetalle detalle : detalles) {
            try {
                BigDecimal precioVenta = detalle.getPrecioUnitario(); 
                BigDecimal precioBase = detalle.getInventario().getProducto().getPrecioBase();
                int cantidad = detalle.getCantidad();
                
                BigDecimal gananciaUnidad = precioVenta.subtract(precioBase);
                BigDecimal gananciaLinea = gananciaUnidad.multiply(new BigDecimal(cantidad));
                
                gananciaTotal = gananciaTotal.add(gananciaLinea);
                
            } catch (Exception e) {
                System.err.println("Error calculando ganancia para detalle ID: " + detalle.getId() + ". Detalles omitido.");
            }
        }
        
        return gananciaTotal;
    }
}