package SCRUM3.Bj_Byte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.model.Proveedor;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import SCRUM3.Bj_Byte.repository.ProveedorRepository;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/producto")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoController(ProductoRepository productoRepository,
                              ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public String listar(Model model, @RequestParam(required = false) String filtro) {
        List<Producto> productos = (filtro == null || filtro.isBlank())
                ? productoRepository.findAll()
                : productoRepository.findAll().stream()
                    .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro.toLowerCase()))
                    .toList();
        model.addAttribute("productos", productos);
        model.addAttribute("filtro", filtro);
        return "producto";
    }

    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "producto_form";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Producto> opt = productoRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/producto";
        }
        model.addAttribute("producto", opt.get());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "producto_form";
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto,
                                  @RequestParam(required = false, name = "proveedorIds") List<Long> proveedorIds,
                                  RedirectAttributes ra) {
        if (proveedorIds != null && !proveedorIds.isEmpty()) {
            Set<Proveedor> provs = new HashSet<>(proveedorRepository.findAllById(proveedorIds));
            producto.setProveedores(provs);
        } else {
            producto.setProveedores(new HashSet<>());
        }
        productoRepository.save(producto);
        ra.addFlashAttribute("mensaje", "Producto guardado");
        return "redirect:/producto";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        productoRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Producto eliminado");
        return "redirect:/producto";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
        nf.setGroupingUsed(true);
        binder.registerCustomEditor(java.math.BigDecimal.class, new CustomNumberEditor(java.math.BigDecimal.class, nf, true));
    }
}
