package SCRUM3.Bj_Byte.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;


@Service
public class InvoicePdfService {

    // Se recomienda usar un logger para seguimiento de errores
    private static final Logger logger = LoggerFactory.getLogger(InvoicePdfService.class);

    private final SpringTemplateEngine templateEngine;

    // Inyección de dependencias a través del constructor
    public InvoicePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renderiza la plantilla Thymeleaf a HTML y la convierte a PDF usando Flying Saucer (ITextRenderer).
     *
     * @param templateName Nombre de la plantilla Thymeleaf (ej: "invoice").
     * @param thymeleafContext Contexto con las variables de datos.
     * @param baseUrl URL base para recursos estáticos (ej: http://localhost:8080) necesaria para resolver imágenes, CSS, etc.
     * @param out El OutputStream donde se escribirá el PDF resultante.
     * @throws Exception Si ocurre un error durante el procesamiento o la generación del PDF.
     */
    public void generatePdf(String templateName, Context thymeleafContext, String baseUrl, OutputStream out) throws Exception {
        
        // 1. Renderizar la plantilla Thymeleaf a una cadena HTML
        logger.info("Renderizando plantilla Thymeleaf: {}", templateName);
        String html = templateEngine.process(templateName, thymeleafContext);

        // 2. Usar Flying Saucer (ITextRenderer) para convertir el HTML a PDF
        ITextRenderer renderer = new ITextRenderer();
        
        // El 'baseUrl' es crucial. Permite que ITextRenderer resuelva rutas de imágenes, como el logo o el QR Base64.
        // El QR Base64 (data:image/png;base64,...) también se maneja correctamente a través de esta configuración.
        renderer.setDocumentFromString(html, baseUrl);
        
        // 3. Aplicar diseño y paginación
        logger.info("Generando layout del PDF.");
        renderer.layout();
        
        // 4. Generar el PDF y escribirlo en el OutputStream
        renderer.createPDF(out);
        out.flush();

        logger.info("PDF generado y escrito en el OutputStream con éxito.");
    }
}