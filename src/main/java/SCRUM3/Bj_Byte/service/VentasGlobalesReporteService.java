package SCRUM3.Bj_Byte.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.VentaDetalle;
import SCRUM3.Bj_Byte.repository.VentaRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de generación de reporte global de ventas.
 *
 * Este servicio produce un PDF textual, monocromo y compacto de una sola página
 * con un resumen ejecutivo y una muestra reducida de transacciones.
 */
@Service
public class VentasGlobalesReporteService {

    @Autowired
    private VentaRepository ventaRepository;

    public void generarReporteVentasGlobales(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_ventas_globales_" + System.currentTimeMillis() + ".pdf");

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            List<Venta> ventas = ventaRepository.findAll();

            agregarEncabezado(document);
            agregarResumenEjecutivo(document, ventas);
            agregarTablaTransaccionesReducida(document, ventas);
            agregarObservacionesYRecomendaciones(document, ventas);

            Paragraph footer = new Paragraph("Fin del resumen. Solicite el informe detallado para información completa.", new Font(Font.HELVETICA, 8));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Error al generar PDF: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void agregarEncabezado(Document document) throws DocumentException {
        Paragraph titulo = new Paragraph("REPORTE GLOBAL DE VENTAS - RESUMEN", new Font(Font.HELVETICA, 16, Font.BOLD));
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph fecha = new Paragraph("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), new Font(Font.HELVETICA, 9));
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(10);
        document.add(fecha);
    }

    private void agregarResumenEjecutivo(Document document, List<Venta> ventas) throws DocumentException {
        Paragraph titulo = new Paragraph("RESUMEN", new Font(Font.HELVETICA, 11, Font.BOLD));
        titulo.setSpacingBefore(6);
        titulo.setSpacingAfter(6);
        document.add(titulo);

        BigDecimal totalVentas = ventas.stream()
                .filter(v -> v.getTotalVenta() != null)
                .map(Venta::getTotalVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantidadVentas = ventas.size();
        BigDecimal promedio = cantidadVentas > 0 ? totalVentas.divide(new BigDecimal(cantidadVentas), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Table tabla = new Table(2);
        tabla.setWidth(100);
        tabla.setPadding(6);

        agregarCeldaTabla(tabla, "Total Ventas", true);
        agregarCeldaTabla(tabla, formatoCurrency(totalVentas), false);

        agregarCeldaTabla(tabla, "Cantidad Ventas", true);
        agregarCeldaTabla(tabla, String.valueOf(cantidadVentas), false);

        agregarCeldaTabla(tabla, "Promedio por Venta", true);
        agregarCeldaTabla(tabla, formatoCurrency(promedio), false);

        document.add(tabla);
        document.add(new Paragraph(" "));
    }

    private void agregarTablaTransaccionesReducida(Document document, List<Venta> ventas) throws DocumentException {
        if (ventas == null || ventas.isEmpty()) return;

        Paragraph titulo = new Paragraph("Muestra de Transacciones (hasta 8)", new Font(Font.HELVETICA, 10, Font.BOLD));
        titulo.setSpacingBefore(6);
        titulo.setSpacingAfter(6);
        document.add(titulo);

        Table tabla = new Table(5);
        tabla.setWidth(100);
        tabla.setPadding(4);

        agregarCeldaTabla(tabla, "ID", true);
        agregarCeldaTabla(tabla, "Empleado", true);
        agregarCeldaTabla(tabla, "Artículos", true);
        agregarCeldaTabla(tabla, "Total", true);
        agregarCeldaTabla(tabla, "Fecha", true);

        List<Venta> muestra = ventas.stream().limit(8).collect(Collectors.toList());
        for (Venta v : muestra) {
            int cantidad = v.getDetalles() != null ? v.getDetalles().stream().mapToInt(VentaDetalle::getCantidad).sum() : 0;
            agregarCeldaTabla(tabla, String.valueOf(v.getId()), false);
            agregarCeldaTabla(tabla, v.getNombreEmpleado() != null ? v.getNombreEmpleado() : "-", false);
            agregarCeldaTabla(tabla, String.valueOf(cantidad), false);
            agregarCeldaTabla(tabla, formatoCurrency(v.getTotalVenta()), false);
            agregarCeldaTabla(tabla, v.getFecha() != null ? v.getFecha().toLocalDate().toString() : "-", false);
        }

        document.add(tabla);
        document.add(new Paragraph(" "));
    }

        private void agregarObservacionesYRecomendaciones(Document document, List<Venta> ventas) throws DocumentException {
        if (ventas == null || ventas.isEmpty()) return;

        Paragraph titulo = new Paragraph("OBSERVACIONES Y RECOMENDACIONES", new Font(Font.HELVETICA, 10, Font.BOLD));
        titulo.setSpacingBefore(6);
        titulo.setSpacingAfter(6);
        document.add(titulo);

        // Calcular artículos totales vendidos
        int totalArticulos = ventas.stream()
            .filter(v -> v.getDetalles() != null)
            .mapToInt(v -> v.getDetalles().stream().mapToInt(VentaDetalle::getCantidad).sum())
            .sum();

        // Determinar empleado con más ventas (por cantidad de transacciones)
        String topEmpleado = ventas.stream()
            .filter(v -> v.getNombreEmpleado() != null)
            .collect(Collectors.groupingBy(Venta::getNombreEmpleado, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("-");

        Paragraph p1 = new Paragraph("Total artículos vendidos: " + totalArticulos, new Font(Font.HELVETICA, 9));
        Paragraph p2 = new Paragraph("Empleado con mayor número de ventas (transacciones): " + topEmpleado, new Font(Font.HELVETICA, 9));
        p1.setSpacingAfter(4);
        p2.setSpacingAfter(6);
        document.add(p1);
        document.add(p2);

        // Texto explicativo y recomendaciones generales
        String texto = "Este documento presenta un resumen ejecutivo de las ventas. " +
            "Los valores mostrados son un extracto para revisión rápida. " +
            "Recomendaciones: revisar los productos con baja rotación, optimizar inventario y programar promociones sobre artículos con alta rotación. " +
            "Para análisis detallados (ventas por producto, tendencias temporales y desglose por cliente), solicite el informe completo.";

        Paragraph p3 = new Paragraph(texto, new Font(Font.HELVETICA, 9));
        p3.setSpacingAfter(6);
        document.add(p3);
        document.add(new Paragraph(" "));
        }

    private void agregarCeldaTabla(Table tabla, String contenido, boolean esEncabezado) throws BadElementException {
        if (esEncabezado) {
            com.lowagie.text.Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD);
            Cell cell = new Cell(new Paragraph(contenido, headerFont));
            cell.setBackgroundColor(new java.awt.Color(60, 60, 60));
            cell.setBorderWidth(0.5f);
            tabla.addCell(cell);
        } else {
            com.lowagie.text.Font normalFont = new Font(Font.HELVETICA, 9);
            Cell cell = new Cell(new Paragraph(contenido, normalFont));
            cell.setBorderWidth(0.3f);
            tabla.addCell(cell);
        }
    }

    private String formatoCurrency(BigDecimal valor) {
        if (valor == null) return "$ 0.00";
        return "$ " + String.format("%,.2f", valor);
    }
}
