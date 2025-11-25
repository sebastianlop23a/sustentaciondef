package SCRUM3.Bj_Byte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import SCRUM3.Bj_Byte.model.Proveedor;
import SCRUM3.Bj_Byte.repository.ProveedorRepository;

import java.util.Optional;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorRepository repo;

    public ProveedorController(ProveedorRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proveedores", repo.findAll());
        return "proveedores";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedor_form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        repo.save(proveedor);
        ra.addFlashAttribute("mensaje", "Proveedor guardado");
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Proveedor> p = repo.findById(id);
        if (p.isEmpty()) {
            ra.addFlashAttribute("error", "Proveedor no encontrado");
            return "redirect:/proveedores";
        }
        model.addAttribute("proveedor", p.get());
        return "proveedor_form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("mensaje", "Proveedor eliminado");
        return "redirect:/proveedores";
    }
}