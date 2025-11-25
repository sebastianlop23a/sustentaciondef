package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Cliente;
import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public String listar(@RequestParam(required = false) String filtro, HttpSession session, Model model) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";

        List<Cliente> clientes;
        if (filtro != null && !filtro.isEmpty()) {
            clientes = clienteService.buscarPorNombre(filtro);
        } else {
            clientes = clienteService.listar();
        }

        model.addAttribute("clientes", clientes);
        model.addAttribute("esAdmin", empleado.getRolId() == 1);
        model.addAttribute("filtro", filtro);
        return "clientes_list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";
        model.addAttribute("cliente", new Cliente());
        return "cliente_form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";
        clienteService.guardar(cliente);
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";
        Cliente cliente = clienteService.buscarPorId(id).orElse(null);
        if (cliente == null) return "redirect:/clientes";
        model.addAttribute("cliente", cliente);
        return "cliente_form";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";
        clienteService.eliminar(id);
        return "redirect:/clientes";
    }
}
