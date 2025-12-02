package SCRUM3.Bj_Byte.service.pdf;

import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.VentaDetalle;
import SCRUM3.Bj_Byte.service.dto.MetricasFinancieras;
import SCRUM3.Bj_Byte.service.exception.InformeFinancieroException;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación mínima y textual del generador de informes PDF.
 * Produce un resumen ejecutivo de una sola página sin gráficos.
 * Diseño: estrictamente monocromo y compacto.
 */
@Component
public class GeneradorPdfInformeImpl implements GeneradorPdfInforme {

    private static final Logger logger = LoggerFactory.getLogger(GeneradorPdfInformeImpl.class);

    @Override
    public void generarInforme(HttpServletResponse response,
                               MetricasFinancieras metricas,
                               List<Venta> ventas,
                               Map<String, BigDecimal> ventasPorEmpleado,
                               Map<String, Integer> distribucionArticulos) throws Exception {
        Document doc = new Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, response.getOutputStream());
            doc.open();

            // Encabezado
            Font title = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph pTitle = new Paragraph("REPORTE FINANCIERO - RESUMEN", title);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTitle);

            Font meta = new Font(Font.HELVETICA, 8);
            Paragraph pMeta = new Paragraph("Empresa: TALLER DE MOTOS BJ-BYTE | Generado: "
                    + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), meta);
            pMeta.setAlignment(Element.ALIGN_CENTER);
            doc.add(pMeta);

            doc.add(new Paragraph(" "));

            // Resumen ejecutivo breve
            Font heading = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 9);

            Paragraph resumen = new Paragraph("Resumen Ejecutivo", heading);
            doc.add(resumen);
            doc.add(new Paragraph("Este documento presenta un resumen compacto de las métricas clave. Para obtener el reporte completo con transacciones completas, solicite la versión detallada.", normal));
            doc.add(new Paragraph(" "));

            // Métricas clave en dos columnas
            Table tabla = new Table(2);
            tabla.setWidth(100);
            tabla.setBorderWidth(0);

            tabla.addCell(new Cell(new Phrase("Total Ventas", heading)));
            tabla.addCell(new Cell(new Phrase(formatoCurrency(metricas.getTotalVentas()), normal)));

            tabla.addCell(new Cell(new Phrase("Total Artículos", heading)));
            tabla.addCell(new Cell(new Phrase(String.valueOf(metricas.getTotalArticulos()), normal)));

            tabla.addCell(new Cell(new Phrase("Transacciones", heading)));
            tabla.addCell(new Cell(new Phrase(String.valueOf(metricas.getTotalTransacciones()), normal)));

            tabla.addCell(new Cell(new Phrase("Promedio por Venta", heading)));
            tabla.addCell(new Cell(new Phrase(formatoCurrency(metricas.getPromedioVenta()), normal)));

            doc.add(tabla);

            doc.add(new Paragraph(" "));

            // Detalle compacto: hasta 8 transacciones
            if (ventas != null && !ventas.isEmpty()) {
                doc.add(new Paragraph("Transacciones (muestra)", heading));
                Table tDet = new Table(5);
                tDet.setWidth(100);
                tDet.addCell(new Cell(new Phrase("ID", heading)));
                tDet.addCell(new Cell(new Phrase("Empleado", heading)));
                tDet.addCell(new Cell(new Phrase("Artículos", heading)));
                tDet.addCell(new Cell(new Phrase("Total", heading)));
                tDet.addCell(new Cell(new Phrase("Fecha", heading)));

                List<Venta> muestra = ventas.stream().limit(8).collect(Collectors.toList());
                for (Venta v : muestra) {
                    int cantidad = v.getDetalles().stream().mapToInt(VentaDetalle::getCantidad).sum();
                    tDet.addCell(new Cell(new Phrase(String.valueOf(v.getId()), normal)));
                    tDet.addCell(new Cell(new Phrase(v.getNombreEmpleado() != null ? v.getNombreEmpleado() : "-", normal)));
                    tDet.addCell(new Cell(new Phrase(String.valueOf(cantidad), normal)));
                    tDet.addCell(new Cell(new Phrase(formatoCurrency(v.getTotalVenta()), normal)));
                    tDet.addCell(new Cell(new Phrase(v.getFecha() != null ? v.getFecha().toLocalDate().toString() : "-", normal)));
                }
                doc.add(tDet);
            }

            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Fin del resumen. Contacte al administrador para el informe detallado.", meta);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

        } catch (DocumentException | IOException e) {
            logger.error("Error generando PDF textual: {}", e.getMessage(), e);
            throw new InformeFinancieroException("PDF_GENERACION_FALLO", "Fallo generando PDF textual", e.getMessage(), e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private String formatoCurrency(BigDecimal valor) {
        if (valor == null) return "$ 0.00";
        return "$ " + String.format("%,.2f", valor);
    }
}
