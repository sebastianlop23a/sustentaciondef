package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Cita;
import SCRUM3.Bj_Byte.repository.CitaRepository;
import SCRUM3.Bj_Byte.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/citas")
public class CitaController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private EmailService emailService;

    // ================================
    // 📌 Agendar cita
    // ================================
    @GetMapping("/agendar")
    public String mostrarFormularioAgendar(Model model) {
        model.addAttribute("cita", new Cita());
        return "agendar-cita";
    }

    @PostMapping("/agendar")
    public String procesarAgendamiento(@RequestParam("fecha") String fecha,
                                       @RequestParam("hora") String hora,
                                       @RequestParam("modelo") String modelo,
                                       @RequestParam("motivo") String motivo,
                                       @RequestParam("descripcion") String descripcion,
                                       @RequestParam("correo") String correo,
                                       Model model) {
        Cita cita = new Cita();
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setModelo(modelo);
        cita.setMotivo(motivo);
        cita.setDescripcion(descripcion);
        cita.setCorreo(correo);
        cita.setEstado("Pendiente");
        citaRepository.save(cita);

        // 📧 Correo individual al agendar
        String asunto = "Nueva Cita Agendada";
        String cuerpo = "Se ha agendado una nueva cita:\n\n" +
                        "Fecha: " + fecha + "\n" +
                        "Hora: " + hora + "\n" +
                        "Modelo: " + modelo + "\n" +
                        "Motivo: " + motivo + "\n" +
                        "Descripción: " + descripcion + "\n" +
                        "Estado: Pendiente";

        emailService.enviarCorreo(correo, asunto, cuerpo);

        model.addAttribute("mensaje", "¡Cita agendada y correo enviado correctamente!");
        return "agendar-cita";
    }

    // ================================
    // 📌 Listar citas
    // ================================
    @GetMapping
    public String listarCitas(Model model) {
        List<Cita> citas = citaRepository.findAll();
        model.addAttribute("citas", citas);
        return "Listar_citas";
    }

    // ================================
    // 📌 Actualizar estado de cita
    // ================================
    @PostMapping("/actualizarEstado/{id}")
    public String actualizarEstado(@PathVariable Long id,
                                   @RequestParam("estado") String estado,
                                   RedirectAttributes redirectAttributes) {
        Cita cita = citaRepository.findById(id).orElse(null);
        if (cita != null) {
            cita.setEstado(estado);
            citaRepository.save(cita);
            redirectAttributes.addFlashAttribute("mensaje", "Estado de la cita actualizado a: " + estado);
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "La cita no existe.");
        }
        return "redirect:/citas";
    }

    // ================================
    // 📌 Subir citas desde CSV
    // ================================
    @PostMapping("/subir-csv")
    public String subirCsv(@RequestParam("file") MultipartFile file,
                           RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Por favor, selecciona un archivo CSV.");
            return "redirect:/citas";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String linea;
            int contador = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] datos = linea.split(",");

                if (datos.length != 6) continue;

                Cita cita = new Cita();
                cita.setFecha(datos[0].trim());
                cita.setHora(datos[1].trim());
                cita.setModelo(datos[2].trim());
                cita.setMotivo(datos[3].trim());
                cita.setDescripcion(datos[4].trim());
                cita.setCorreo(datos[5].trim());
                cita.setEstado("Pendiente");

                citaRepository.save(cita);
                contador++;
            }

            redirectAttributes.addFlashAttribute("mensaje", "Se subieron " + contador + " citas desde el CSV.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al procesar el archivo: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    // ================================
    // 📌 Enviar correo de una cita específica
    // ================================
    @GetMapping("/enviar-correo/{id}")
    public String enviarCorreoCita(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Cita cita = citaRepository.findById(id).orElse(null);

        if (cita == null) {
            redirectAttributes.addFlashAttribute("mensaje", "La cita no existe.");
            return "redirect:/citas";
        }

        String asunto = "Detalles de tu cita agendada";
        String cuerpo = "Estos son los detalles de tu cita:\n\n" +
                        "Fecha: " + cita.getFecha() + "\n" +
                        "Hora: " + cita.getHora() + "\n" +
                        "Modelo: " + cita.getModelo() + "\n" +
                        "Motivo: " + cita.getMotivo() + "\n" +
                        "Descripción: " + cita.getDescripcion() + "\n" +
                        "Estado: " + cita.getEstado();

        emailService.enviarCorreo(cita.getCorreo(), asunto, cuerpo);

        redirectAttributes.addFlashAttribute("mensaje", "Correo enviado correctamente a " + cita.getCorreo());
        return "redirect:/citas";
    }

    // ================================
    // 📌 Enviar correos masivos a todas las citas pendientes
    // ================================
    @GetMapping("/enviar-correo-masivo")
    public String enviarCorreoMasivo(RedirectAttributes redirectAttributes) {
        List<Cita> pendientes = citaRepository.findByEstado("Pendiente");

        if (pendientes.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "No hay citas pendientes para enviar correos.");
            return "redirect:/citas";
        }

        List<String> destinatarios = pendientes.stream()
                .map(Cita::getCorreo)
                .collect(Collectors.toList());

        String asunto = "Recordatorio de cita pendiente";
        String cuerpo = "Estimado usuario,\n\nTiene una cita pendiente en nuestro sistema.\n\n" +
                        "Por favor, revise los detalles en su cuenta.\n\nSaludos,\nEl equipo de soporte.";

        emailService.enviarCorreoMasivo(destinatarios, asunto, cuerpo);

        redirectAttributes.addFlashAttribute("mensaje", "Se enviaron correos masivos a " + destinatarios.size() + " citas pendientes.");
        return "redirect:/citas";
    }
//elimiar cita
@PostMapping("/eliminar-por-datos")
public String eliminarCitaPorDatos(
        @RequestParam("fecha") String fecha,
        @RequestParam("hora") String hora,
        @RequestParam("correo") String correo,
        RedirectAttributes redirectAttributes) {

    List<Cita> citas = citaRepository.findByFechaAndHoraAndCorreo(fecha, hora, correo);

    if (citas.isEmpty()) {
        redirectAttributes.addFlashAttribute("mensaje", "No se encontró ninguna cita con esos datos.");
    } else {
        // Si hay varias coincidencias, eliminamos todas o la primera
        citas.forEach(citaRepository::delete);
        redirectAttributes.addFlashAttribute("mensaje", "Cita(s) eliminada(s) correctamente.");
    }

    return "redirect:/citas";
}

}