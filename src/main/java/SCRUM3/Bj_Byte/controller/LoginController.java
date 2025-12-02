package SCRUM3.Bj_Byte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.repository.EmpleadoRepository;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/empleados")
public class LoginController {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mostrar formulario de login
    @GetMapping("/login")
    public String mostrarFormularioLogin(HttpSession session) {
        if (session.getAttribute("empleadoLogueado") != null) {
            return "redirect:/home"; 
        }
        return "index";
    }

    // Procesar login
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                 @RequestParam("password") String contrasena,
                                 HttpSession session,
                                 Model model) {

        // Buscar empleado por correo
        Empleado empleado = empleadoRepository.findByCorreo(correo).orElse(null);

        // Validar existencia y estado activo
        if (empleado != null && empleado.isActivo() && 
            passwordEncoder.matches(contrasena, empleado.getContrasena())) {

            session.setAttribute("empleadoLogueado", empleado);
            session.setAttribute("empleadoId", empleado.getId());
            return "redirect:/home"; // ✅ Redirige al home global
        } 
        else if (empleado != null && !empleado.isActivo()) {
            model.addAttribute("error", "El empleado está inactivo. Contacta al administrador.");
            return "index";
        } 
        else {
            model.addAttribute("error", "Correo o contraseña incorrectos");
            return "index";
        }
    }

    // Cerrar sesión
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/empleados/login";
    }
}
