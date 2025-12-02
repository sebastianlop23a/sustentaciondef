package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/correo")
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * Enviar un solo correo
     * POST /correo/enviar
     */
    @PostMapping("/enviar")
    public String enviarCorreo(
            @RequestParam String para,
            @RequestParam String asunto,
            @RequestParam String cuerpo
    ) {
        emailService.enviarCorreo(para, asunto, cuerpo);
        return "Correo enviado a: " + para;
    }

    /**
     * Envío masivo uno por uno
     * POST /correo/masivo
     */
    @PostMapping("/masivo")
    public String enviarCorreoMasivo(
            @RequestBody List<String> destinatarios,
            @RequestParam String asunto,
            @RequestParam String cuerpo
    ) {
        emailService.enviarCorreoMasivo(destinatarios, asunto, cuerpo);
        return "Envío masivo completado. Destinatarios: " + destinatarios.size();
    }

    /**
     * Envío masivo con BCC
     * POST /correo/masivo-bcc
     */
    @PostMapping("/masivo-bcc")
    public String enviarCorreoMasivoBCC(
            @RequestBody List<String> destinatarios,
            @RequestParam String asunto,
            @RequestParam String cuerpo
    ) {
        emailService.enviarCorreoMasivoBCC(destinatarios, asunto, cuerpo);
        return "Envío masivo BCC completado. Destinatarios: " + destinatarios.size();
    }
}
