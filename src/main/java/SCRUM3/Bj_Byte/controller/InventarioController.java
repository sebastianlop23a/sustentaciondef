package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Inventario;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import SCRUM3.Bj_Byte.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/inventario")
    public String verInventario(Model model) {
    // Filtrar inventarios y productos para excluir productos desactivados
    List<Inventario> inventarios = inventarioService.listar().stream()
        .filter(inv -> inv.getProducto() != null && Boolean.TRUE.equals(inv.getProducto().getActivo()))
        .collect(Collectors.toList());

    List<Producto> productos = productoRepository.findAll().stream()
        .filter(p -> Boolean.TRUE.equals(p.getActivo()))
        .collect(Collectors.toList());

    model.addAttribute("inventarios", inventarios);
    model.addAttribute("productos", productos); // listado para seleccionar producto al crear inventario (solo activos)
        return "inventario";
    }

    @PostMapping("/inventario/agregar")
    public String agregarInventario(@RequestParam Long productoId,
                                    @RequestParam String ubicacion,
                                    @RequestParam String estado,
                                    @RequestParam int cantidad,
                                    @RequestParam(required = false) String observaciones,
                                    RedirectAttributes redirectAttributes) {

        // 🚫 Validar que la cantidad sea positiva
        if (cantidad <= 0) {
            redirectAttributes.addFlashAttribute("error", "La cantidad debe ser mayor a 0");
            return "redirect:/inventario";
        }

        // ✅ Verificar si ya existe inventario con mismo producto + ubicación + estado
        Optional<Inventario> existente = inventarioService.buscarPorProductoUbicacionEstado(productoId, ubicacion, estado);

        if (existente.isPresent()) {
            Inventario inv = existente.get();
            inv.setCantidad(inv.getCantidad() + cantidad);

            // Concatenar observaciones si se agregan nuevas
            if (observaciones != null && !observaciones.trim().isEmpty()) {
                String obsActual = inv.getObservaciones() != null ? inv.getObservaciones() + " | " : "";
                inv.setObservaciones(obsActual + observaciones);
            }

            inventarioService.guardar(inv);
        } else {
            // Si no existe, se crea un nuevo registro
            Inventario nuevo = new Inventario();
            productoRepository.findById(productoId).ifPresent(nuevo::setProducto);
            nuevo.setUbicacion(ubicacion);
            nuevo.setEstado(estado);
            nuevo.setCantidad(cantidad);
            nuevo.setObservaciones(observaciones);
            inventarioService.guardar(nuevo);
        }

        redirectAttributes.addFlashAttribute("success", "Entrada registrada correctamente");
        return "redirect:/inventario";
    }
}
