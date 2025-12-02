package SCRUM3.Bj_Byte.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Cargar remitente desde application.properties
    @Value("${spring.mail.username}")
    private String remitente;

    /**
     * Enviar correo a un destinatario
     */
    public void enviarCorreo(String para, String asunto, String cuerpo) {

        if (para == null || para.trim().isEmpty()) {
            log.error("Intento de enviar correo sin destinatario.");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        try {
            mailSender.send(mensaje);
            log.info("Correo enviado correctamente a {}", para);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", para, e.getMessage());
        }
    }

    /**
     * Enviar correo a varios destinatarios (uno por uno)
     */
    public void enviarCorreoMasivo(List<String> destinatarios, String asunto, String cuerpo) {

        if (destinatarios == null || destinatarios.isEmpty()) {
            log.warn("Lista de destinatarios vacía en envío masivo.");
            return;
        }

        destinatarios.forEach(dest -> enviarCorreo(dest, asunto, cuerpo));
    }

    /**
     * Enviar correo masivo usando BCC
     */
    public void enviarCorreoMasivoBCC(List<String> destinatarios, String asunto, String cuerpo) {

        if (destinatarios == null || destinatarios.isEmpty()) {
            log.warn("No hay destinatarios para envío masivo BCC.");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setBcc(destinatarios.toArray(new String[0]));
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        try {
            mailSender.send(mensaje);
            log.info("Correo masivo enviado a {} destinatarios (BCC).", destinatarios.size());
        } catch (Exception e) {
            log.error("Error en envío masivo BCC: {}", e.getMessage());
        }
    }
}
