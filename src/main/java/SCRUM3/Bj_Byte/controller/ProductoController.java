package SCRUM3.Bj_Byte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.model.Proveedor;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import SCRUM3.Bj_Byte.repository.ProveedorRepository;
import SCRUM3.Bj_Byte.service.ProductoService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.Locale;

@Controller
@RequestMapping("/producto")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoService productoService;

    public ProductoController(ProductoRepository productoRepository,
                              ProveedorRepository proveedorRepository,
                              ProductoService productoService) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoService = productoService;
    }

    // ================================
    // LISTAR PRODUCTOS
    // ================================
    @GetMapping
    public String listar(Model model, @RequestParam(required = false) String filtro, HttpSession session) {
        Object empleadoObj = session.getAttribute("empleadoLogueado");
        
        List<Producto> productos = (filtro == null || filtro.isBlank())
                ? productoRepository.findAll()
                : productoRepository.findAll().stream()
                    .filter(p -> p.getNombre() != null &&
                            p.getNombre().toLowerCase().contains(filtro.toLowerCase()))
                    .toList();

        System.out.println("DEBUG LISTAR: Total productos en BD: " + productos.size());
        if (!productos.isEmpty()) {
            System.out.println("Primer producto: " + productos.get(0).getNombre() + " | Precio: " + productos.get(0).getPrecio());
        }

        model.addAttribute("productos", productos);
        model.addAttribute("filtro", filtro);
        model.addAttribute("empleadoLogueado", empleadoObj);

        return "producto";
    }

    // ================================
    // NUEVO PRODUCTO
    // ================================
    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "producto_form";
    }

    // ================================
    // EDITAR PRODUCTO
    // ================================
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

    // ================================
    // GUARDAR PRODUCTO
    // ================================
    @PostMapping("/guardar")
    public String guardarProducto(
            @ModelAttribute Producto producto,
            @RequestParam(required = false, name = "proveedorIds") List<Long> proveedorIds,
            RedirectAttributes ra) {

        // Asignar proveedores
        if (proveedorIds != null && !proveedorIds.isEmpty()) {
            Set<Proveedor> provs = new HashSet<>(proveedorRepository.findAllById(proveedorIds));
            producto.setProveedores(provs);
        } else {
            producto.setProveedores(new HashSet<>());
        }

        // Cálculo automático de ganancia
        if (producto.getPrecioBase() != null && producto.getPrecio() != null) {
            BigDecimal ganancia = producto.getPrecio().subtract(producto.getPrecioBase());
            producto.setGanancia(ganancia);
        }

        productoRepository.save(producto);
        ra.addFlashAttribute("mensaje", "Producto guardado correctamente");

        return "redirect:/producto";
    }

    // ================================
    // ELIMINAR PRODUCTO
    // ================================
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        productoRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Producto eliminado");
        return "redirect:/producto";
    }

    // ================================
    // CARGAR CSV
    // ================================
    @PostMapping("/cargar-csv")
    public String cargarCSV(@RequestParam("archivo") MultipartFile archivo,
                            RedirectAttributes ra,
                            HttpSession session) {
        if (archivo.isEmpty()) {
            ra.addFlashAttribute("error", "El archivo está vacío");
            return "redirect:/producto";
        }

        try {
            productoService.cargarProductosDesdeCSV(archivo.getInputStream());
            ra.addFlashAttribute("mensaje", "Productos cargados correctamente");
            System.out.println("✓ Carga masiva completada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al cargar CSV: " + e.getMessage());
            System.err.println("✗ Error en carga masiva: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/producto";
    }

    // ================================
    // FORMATEO DE NUMEROS (BIGDECIMAL)
    // ================================
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
        nf.setGroupingUsed(true);

        binder.registerCustomEditor(
                BigDecimal.class,
                new CustomNumberEditor(BigDecimal.class, nf, true)
        );
    }
}
