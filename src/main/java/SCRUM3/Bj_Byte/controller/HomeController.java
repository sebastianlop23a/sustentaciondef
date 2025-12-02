package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.model.Inventario;
import SCRUM3.Bj_Byte.repository.VentaRepository;
import SCRUM3.Bj_Byte.repository.CitaRepository;
import SCRUM3.Bj_Byte.repository.InventarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private VentaRepository ventaRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private InventarioRepository inventarioRepo;

    // ==========================
    // 📌 Redirige desde la raíz al login/index
    // ==========================
    @GetMapping("/")
    public String redirigirAlLogin() {
        return "redirect:/empleados/login";
    }

    // ==========================
    // 📊 Vista del Dashboard/Home
    // ==========================
    @GetMapping("/home")
    public String mostrarHome(HttpSession session, Model model) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");

        // Si no hay sesión activa, redirige al login
        if (empleado == null) {
            return "redirect:/empleados/login";
        }

        // ==========================
        // 📊 Cálculos para el dashboard
        // ==========================

        // Rango de hoy (semi-abierto: [inicioHoy, inicioMañana) )
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();

        // Total de ventas de HOY
        BigDecimal ventasHoy = ventaRepo.totalVentasEnRango(inicioHoy, inicioManana);
        ventasHoy = ventasHoy != null ? ventasHoy : BigDecimal.ZERO;

        // Total de ventas HISTÓRICAS
        BigDecimal ventasHistoricas = ventaRepo.totalVentasHistoricas();
        ventasHistoricas = ventasHistoricas != null ? ventasHistoricas : BigDecimal.ZERO;

        // Total de citas pendientes
        Long citasPendientes = citaRepo.countByEstado("Pendiente");
        citasPendientes = citasPendientes != null ? citasPendientes : 0L;

        // Total de productos en inventario
        Long productosTotales = inventarioRepo.count();
        productosTotales = productosTotales != null ? productosTotales : 0L;

        // NOTA: EL CÁLCULO DE GANANCIA HISTÓRICA SE HA MOVIDO AL GANANCIA CONTROLLER

        // ==========================
        // 📊 Ventas de la semana (Lunes a Domingo)
        // ==========================
        List<Double> ventasSemana = new ArrayList<>();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY); // inicio de semana (lunes)
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            LocalDateTime inicioDia = dia.atStartOfDay();
            LocalDateTime finDia = dia.plusDays(1).atStartOfDay();

            BigDecimal totalDia = ventaRepo.totalVentasEnRango(inicioDia, finDia);
            ventasSemana.add(totalDia != null ? totalDia.doubleValue() : 0.0);
        }

        // ==========================
        // 🚨 Productos con bajo stock
        // ==========================
        List<Inventario> productosBajos = inventarioRepo.findByCantidadLessThan(5);
        if (productosBajos == null) productosBajos = new ArrayList<>();
        model.addAttribute("productosBajos", productosBajos);

        // ==========================
        // 📌 Pasar datos al modelo
        // ==========================
        model.addAttribute("empleado", empleado);
        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("ventasHistoricas", ventasHistoricas);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("productosTotales", productosTotales);
        model.addAttribute("ventasSemana", ventasSemana);
        // NO se pasa gananciaTotalHistorica

        return "home"; // apunta a templates/home.html
    }
}
