package SCRUM3.Bj_Byte.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;

    public InvoicePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renderiza la plantilla Thymeleaf a HTML y la convierte a PDF.
     * baseUrl debe ser la URL base para recursos (ej: http://localhost:8080 o "" si usas rutas absolutas)
     */
    public void generatePdf(String templateName, Context thymeleafContext, String baseUrl, OutputStream out) throws Exception {
        // renderizar HTML
        String html = templateEngine.process(templateName, thymeleafContext);

        // usar Flying Saucer para convertir a PDF
        ITextRenderer renderer = new ITextRenderer();
        // importante: pasa baseUrl para que images(/images/logo.jpg) resuelvan correctamente
        renderer.setDocumentFromString(html, baseUrl);
        renderer.layout();
        renderer.createPDF(out);
        out.flush();
    }
}