package SCRUM3.Bj_Byte.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.VentaRepository;


import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.awt.image.BufferedImage;
import com.lowagie.text.Image;
import java.util.stream.Collectors;

@Service
public class ReporteFinancieroService {

    @Autowired
    private VentaRepository ventaRepository;

    // @Autowired
    // private ProductoRepository productoRepository; // Actualmente no utilizado

    public void generarReporteFinancieroCompleto(HttpServletResponse response,
                                                   String filtroProducto,
                                                   String filtroEmpleado,
                                                   String filtroFecha) throws IOException {

        // Configurar respuesta HTTP ANTES de crear el documento
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_financiero_" + System.currentTimeMillis() + ".pdf");

        try {
            // Usar A4 en orientación vertical (portrait) para un formato más legible
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
            // Añadir header/footer profesional
            writer.setPageEvent(new HeaderFooter());
            document.open();

            // Obtener datos filtrados
            java.util.List<Venta> ventas = obtenerVentasFiltradas(filtroProducto, filtroEmpleado, filtroFecha);

            // Agregar solo los productos vendidos (agregado desde ventas)
            java.util.List<ProductoAggregate> productosVendidos = agregarProductosVendidos(ventas);

            // 1. ENCABEZADO Y TÍTULO
            agregarEncabezado(document);

            // 2. RESUMEN EJECUTIVO (incluye gráfico por empleado)
            agregarResumenEjecutivo(document, ventas, productosVendidos);

            // 3. TABLA DE PRODUCTOS (solo vendidos) - limitada para ajuste en 2 páginas
            java.util.List<ProductoAggregate> topProductos = productosVendidos;
            if (topProductos == null) topProductos = new ArrayList<>();
            int limit = Math.min(10, topProductos.size());
            java.util.List<ProductoAggregate> listadoParaTabla = topProductos.subList(0, limit);
            agregarTablaProductos(document, listadoParaTabla);

            // Nueva página: análisis y producto destacado
            document.newPage();

            // 4. ANÁLISIS DE GANANCIAS (con agregados de vendidos)
            agregarAnalisisGanancias(document, productosVendidos, ventas);

            // 5. PRODUCTO CON MAYOR MARGEN (entre vendidos)
            agregarProductoMayorMargen(document, productosVendidos);

            document.close();

        } catch (DocumentException e) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Error al generar reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para header/footer con estilo profesional.
     */
    private static class HeaderFooter extends com.lowagie.text.pdf.PdfPageEventHelper {
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footer = new PdfPTable(3);
                footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                footer.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
                footer.addCell(new Phrase("TALLER DE MOTOS BJ-BYTE", footerFont));
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
                footer.addCell(new Phrase("Reporte Financiero", footerFont));
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
                footer.addCell(new Phrase(String.format("Página %d", writer.getPageNumber()), footerFont));
                footer.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() - 5, writer.getDirectContent());
            } catch (Exception e) {
                // no crash on footer
            }
        }
    }

    // Clase auxiliar para agregados de productos vendidos
    private static class ProductoAggregate {
        Producto producto;
        int cantidadVendida = 0;
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalGanancia = BigDecimal.ZERO;
        BigDecimal precioPromedio = BigDecimal.ZERO;

        ProductoAggregate(Producto producto) {
            this.producto = producto;
        }
    }

    private java.util.List<ProductoAggregate> agregarProductosVendidos(java.util.List<Venta> ventas) {
        Map<Long, ProductoAggregate> map = new HashMap<>();
        for (Venta v : ventas) {
            if (v.getDetalles() == null) continue;
            for (var d : v.getDetalles()) {
                if (d.getInventario() == null || d.getInventario().getProducto() == null) continue;
                Producto p = d.getInventario().getProducto();
                Long id = p.getId();
                ProductoAggregate agg = map.get(id);
                if (agg == null) {
                    agg = new ProductoAggregate(p);
                    map.put(id, agg);
                }
                int qty = d.getCantidad();
                agg.cantidadVendida += qty;
                BigDecimal linea = BigDecimal.ZERO;
                if (d.getTotalLinea() != null) linea = d.getTotalLinea();
                else if (d.getPrecioUnitario() != null) linea = d.getPrecioUnitario().multiply(new BigDecimal(qty));
                agg.totalVentas = agg.totalVentas.add(linea);
                BigDecimal costoUnit = p.getPrecioBase() != null ? p.getPrecioBase() : BigDecimal.ZERO;
                BigDecimal precioUnit = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : (p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO);
                BigDecimal gananciaLinea = precioUnit.subtract(costoUnit).multiply(new BigDecimal(qty));
                agg.totalGanancia = agg.totalGanancia.add(gananciaLinea);
            }
        }
        // calcular precio promedio
        for (ProductoAggregate a : map.values()) {
            if (a.cantidadVendida > 0) {
                a.precioPromedio = a.totalVentas.divide(new BigDecimal(a.cantidadVendida), 2, RoundingMode.HALF_UP);
            }
        }
        return map.values().stream()
                .sorted((a, b) -> b.totalVentas.compareTo(a.totalVentas))
                .collect(Collectors.toList());
    }

    private void agregarEncabezado(Document document) throws DocumentException {
        // Logo/Nombre del proyecto
        Paragraph proyecto = new Paragraph("TALLER DE MOTOS BJ-BYTE", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Color.DARK_GRAY));
        proyecto.setAlignment(Element.ALIGN_CENTER);
        document.add(proyecto);

        Paragraph subtitle = new Paragraph("Reporte Financiero Detallado", 
            FontFactory.getFont(FontFactory.HELVETICA, 14));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);

        // Fecha de generación
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Paragraph fecha = new Paragraph("Fecha de Generación: " + ahora.format(formatter), 
            FontFactory.getFont(FontFactory.HELVETICA, 10));
        fecha.setAlignment(Element.ALIGN_CENTER);
        document.add(fecha);

        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
    }

    private void agregarResumenEjecutivo(Document document, java.util.List<Venta> ventas, java.util.List<ProductoAggregate> productosVendidos) 
        throws DocumentException {
        
        Paragraph titulo = new Paragraph("RESUMEN EJECUTIVO", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY));
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        // Cálculos
        BigDecimal ingresosTotales = ventas.stream()
            .map(Venta::getTotalVenta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Costos basados en productos vendidos (cantidad * precio base)
        BigDecimal costosTotales = productosVendidos.stream()
            .map(a -> {
                BigDecimal precioBase = a.producto.getPrecioBase() != null ? a.producto.getPrecioBase() : BigDecimal.ZERO;
                return precioBase.multiply(new BigDecimal(a.cantidadVendida));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaTotal = ingresosTotales.subtract(costosTotales);
        BigDecimal margenPromedio = ingresosTotales.compareTo(BigDecimal.ZERO) > 0
            ? gananciaTotal.multiply(new BigDecimal(100)).divide(ingresosTotales, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Tabla de resumen
        Table tablaResumen = new Table(2);
        tablaResumen.setWidth(100);
        tablaResumen.setPadding(8);
        tablaResumen.setBorderColor(Color.GRAY);

        agregarFilaTabla(tablaResumen, "MÉTRICA", "VALOR", true);
        agregarFilaTabla(tablaResumen, "Total de Productos Vendidos (únicos)", String.valueOf(productosVendidos.size()), false);
        agregarFilaTabla(tablaResumen, "Total de Transacciones", String.valueOf(ventas.size()), false);
        agregarFilaTabla(tablaResumen, "Ingresos Totales", formatoCurrency(ingresosTotales), false);
        agregarFilaTabla(tablaResumen, "Ganancia Total", formatoCurrency(gananciaTotal), false);
        agregarFilaTabla(tablaResumen, "Margen Promedio", margenPromedio.toString() + "%", false);
        agregarFilaTabla(tablaResumen, "Ticket Promedio", 
            formatoCurrency(calcularPromedioTicket(ventas)), false);

        document.add(tablaResumen);

        // Texto descriptivo
        document.add(new Paragraph(" "));
        Paragraph descriptivo = new Paragraph(
            "Este reporte presenta un análisis integral de la actividad de ventas (solo productos vendidos). " +
            "Incluye un resumen ejecutivo con totales, un gráfico por empleado y un listado de los productos que " +
            "realmente se vendieron en el período." ,
            FontFactory.getFont(FontFactory.HELVETICA, 10));
        descriptivo.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(descriptivo);

        // Agregar tabla de ventas por empleado y gráfico (escalado al ancho utilizable)
        document.add(new Paragraph(" "));
        agregarVentasPorEmpleadoTabla(document, ventas);
        int usableWidth = (int) (document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
        com.lowagie.text.Image chart = crearGraficoVentasPorEmpleado(ventas, usableWidth, 200);
        if (chart != null) {
            chart.scaleToFit(usableWidth, 200);
            chart.setAlignment(Element.ALIGN_CENTER);
            document.add(chart);
        }
    }

    private void agregarTablaProductos(Document document, java.util.List<ProductoAggregate> productos) 
        throws DocumentException {
        
        Paragraph titulo = new Paragraph("RESUMEN DE PRODUCTOS", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY));
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        Table tabla = new Table(7);
        tabla.setWidth(100);
        tabla.setPadding(6);
        tabla.setBorderColor(Color.GRAY);

        // Encabezados
        agregarFilaTabla(tabla, "NOMBRE", "DESCRIPCIÓN", "PRECIO BASE", "PRECIO VENTA PROM.", "GANANCIA/U", "EXENTO", "CANTIDAD", true);

        // Filas
        for (ProductoAggregate a : productos) {
            Producto p = a.producto;
            if (p == null) continue;
            String nombre = p.getNombre() != null ? p.getNombre() : "N/A";
            String desc = p.getDescripcion() != null ? p.getDescripcion() : "";
            String descripcion = desc.length() > 30 ? desc.substring(0, 30) + "..." : desc;
            BigDecimal precioBase = p.getPrecioBase() != null ? p.getPrecioBase() : BigDecimal.ZERO;
            BigDecimal precioProm = a.precioPromedio != null ? a.precioPromedio : BigDecimal.ZERO;
            BigDecimal gananciaUnidad = precioProm.subtract(precioBase);
            String exento = p.getExento() != null && p.getExento() ? "Sí" : "No";
            agregarFilaTabla(tabla,
                nombre,
                descripcion,
                formatoCurrency(precioBase),
                formatoCurrency(precioProm),
                formatoCurrency(gananciaUnidad),
                exento,
                String.valueOf(a.cantidadVendida),
                false);
        }

        document.add(tabla);
    }

    private void agregarFilaTabla(Table tabla, String col1, String col2, String col3, String col4, 
                                   String col5, String col6, String col7, boolean esEncabezado) 
        throws BadElementException {
        
        String[] valores = {col1, col2, col3, col4, col5, col6, col7};
        
        for (int i = 0; i < 7; i++) {
            if (esEncabezado) {
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
                Paragraph p = new Paragraph(valores[i], headerFont);
                Cell cell = new Cell(p);
                cell.setBackgroundColor(new Color(60, 60, 60));
                
                tabla.addCell(cell);
            } else {
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
                Cell cell = new Cell(new Paragraph(valores[i], normalFont));
                tabla.addCell(cell);
            }
        }
    }

    private void agregarAnalisisGanancias(Document document, java.util.List<ProductoAggregate> productos, java.util.List<Venta> ventas) 
        throws DocumentException {
        
        Paragraph titulo = new Paragraph("ANÁLISIS DE GANANCIAS", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY));
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        // Estadísticas generales
        Table tablaEstadisticas = new Table(2);
        tablaEstadisticas.setWidth(100);
        tablaEstadisticas.setPadding(8);
        tablaEstadisticas.setBorderColor(Color.GRAY);

        agregarFilaTabla(tablaEstadisticas, "CONCEPTO", "VALOR", true);

        // Ganancia promedio por unidad usando agregados
        BigDecimal sumaGanancia = productos.stream()
            .map(a -> a.totalGanancia != null ? a.totalGanancia : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int sumaCantidad = productos.stream().mapToInt(a -> a.cantidadVendida).sum();
        BigDecimal gananciaPromedio = sumaCantidad > 0
            ? sumaGanancia.divide(new BigDecimal(sumaCantidad), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Margen promedio ponderado por ventas
        BigDecimal margenPromedio = BigDecimal.ZERO;
        if (sumaCantidad > 0) {
            BigDecimal ingresos = productos.stream().map(a -> a.totalVentas != null ? a.totalVentas : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costos = productos.stream().map(a -> {
                BigDecimal base = a.producto.getPrecioBase() != null ? a.producto.getPrecioBase() : BigDecimal.ZERO;
                return base.multiply(new BigDecimal(a.cantidadVendida));
            }).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ganTotal = ingresos.subtract(costos);
            if (ingresos.compareTo(BigDecimal.ZERO) > 0) {
                margenPromedio = ganTotal.multiply(new BigDecimal(100)).divide(ingresos, 2, RoundingMode.HALF_UP);
            }
        }

        agregarFilaTabla(tablaEstadisticas, "Ganancia Promedio por Unidad", formatoCurrency(gananciaPromedio), false);
        agregarFilaTabla(tablaEstadisticas, "Margen Promedio Ponderado", margenPromedio.toString() + "%", false);
        long mayores30 = contarProductosPorMargenAggregate(productos, 30, 1000);
        agregarFilaTabla(tablaEstadisticas, "Productos con Mayor Margen (>30%)", String.valueOf(mayores30), false);

        document.add(tablaEstadisticas);

        document.add(new Paragraph(" "));

        // Desglose por rango de ganancia
        Paragraph subtitulo = new Paragraph("Distribución por Rango de Margen de Ganancia:", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        document.add(subtitulo);

        Table tablaDesglose = new Table(2);
        tablaDesglose.setWidth(100);
        tablaDesglose.setPadding(6);
        tablaDesglose.setBorderColor(Color.GRAY);

        agregarFilaTabla(tablaDesglose, "RANGO DE MARGEN", "CANTIDAD DE PRODUCTOS", true);

        long menores10 = contarProductosPorMargenAggregate(productos, 0, 10);
        long entre10y20 = contarProductosPorMargenAggregate(productos, 10, 20);
        long entre20y30 = contarProductosPorMargenAggregate(productos, 20, 30);
        // long mayores30 already computed above

        agregarFilaTabla(tablaDesglose, "0% - 10%", String.valueOf(menores10), false);
        agregarFilaTabla(tablaDesglose, "10% - 20%", String.valueOf(entre10y20), false);
        agregarFilaTabla(tablaDesglose, "20% - 30%", String.valueOf(entre20y30), false);
        agregarFilaTabla(tablaDesglose, ">30%", String.valueOf(mayores30), false);

        document.add(tablaDesglose);
    }

    private void agregarProductoMayorMargen(Document document, java.util.List<ProductoAggregate> productos) 
        throws DocumentException {
        
        Paragraph titulo = new Paragraph("PRODUCTO CON MAYOR MARGEN DE GANANCIA", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY));
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        // Encontrar producto con mayor margen usando agregados
        ProductoAggregate mayor = productos.stream()
            .filter(a -> a.producto != null && a.producto.getActivo())
            .filter(a -> a.producto.getPrecioBase() != null && a.producto.getPrecioBase().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(a -> {
                BigDecimal precioProm = a.precioPromedio != null ? a.precioPromedio : BigDecimal.ZERO;
                BigDecimal base = a.producto.getPrecioBase() != null ? a.producto.getPrecioBase() : BigDecimal.ZERO;
                return precioProm.subtract(base)
                    .multiply(new BigDecimal(100))
                    .divide(base, 2, RoundingMode.HALF_UP);
            }))
            .orElse(null);

        if (mayor != null && mayor.producto != null) {
            Producto productoMayor = mayor.producto;
            BigDecimal precioProm = mayor.precioPromedio != null ? mayor.precioPromedio : BigDecimal.ZERO;
            BigDecimal base = productoMayor.getPrecioBase() != null ? productoMayor.getPrecioBase() : BigDecimal.ZERO;
            BigDecimal margen = base.compareTo(BigDecimal.ZERO) > 0
                ? precioProm.subtract(base).multiply(new BigDecimal(100)).divide(base, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            Table tablaProducto = new Table(2);
            tablaProducto.setWidth(100);
            tablaProducto.setPadding(8);
            tablaProducto.setBorderColor(Color.GRAY);

            agregarFilaTabla(tablaProducto, "ATRIBUTO", "VALOR", true);
            agregarFilaTabla(tablaProducto, "Nombre", productoMayor.getNombre(), false);
            agregarFilaTabla(tablaProducto, "Descripción", 
                productoMayor.getDescripcion() != null ? productoMayor.getDescripcion() : "N/A", false);
            agregarFilaTabla(tablaProducto, "Precio Base", formatoCurrency(base), false);
            agregarFilaTabla(tablaProducto, "Precio de Venta Prom.", formatoCurrency(precioProm), false);
            agregarFilaTabla(tablaProducto, "Ganancia por Unidad", 
                formatoCurrency(precioProm.subtract(base)), false);
            agregarFilaTabla(tablaProducto, "Margen de Ganancia", margen.toString() + "%", false);
            agregarFilaTabla(tablaProducto, "Cantidad Vendida", String.valueOf(mayor.cantidadVendida), false);

            document.add(tablaProducto);

            document.add(new Paragraph(" "));

            Paragraph analisis = new Paragraph(
                "Este producto representa la mejor oportunidad de ganancia entre los productos vendidos, " +
                "con un margen del " + margen + "%. Se recomienda evaluar inventario y promoción para este ítem.",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
            analisis.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(analisis);
        } else {
            Paragraph sinDatos = new Paragraph("No hay productos vendidos para analizar.",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
            document.add(sinDatos);
        }
    }

    private long contarProductosPorMargenAggregate(java.util.List<ProductoAggregate> productos, int margenMin, int margenMax) {
        return productos.stream()
            .filter(a -> a.producto != null && a.producto.getActivo())
            .filter(a -> a.producto.getPrecioBase() != null && a.producto.getPrecioBase().compareTo(BigDecimal.ZERO) > 0)
            .filter(a -> {
                BigDecimal precioProm = a.precioPromedio != null ? a.precioPromedio : BigDecimal.ZERO;
                BigDecimal base = a.producto.getPrecioBase();
                BigDecimal margen = precioProm.subtract(base)
                    .multiply(new BigDecimal(100)).divide(base, 2, RoundingMode.HALF_UP);
                return margen.compareTo(new BigDecimal(margenMin)) >= 0 && margen.compareTo(new BigDecimal(margenMax)) < 0;
            })
            .count();
    }

    private void agregarVentasPorEmpleadoTabla(Document document, java.util.List<Venta> ventas) throws DocumentException {
        Table tabla = new Table(3);
        tabla.setWidth(100);
        tabla.setPadding(6);
        tabla.setBorderColor(Color.GRAY);
        agregarFilaTabla(tabla, "EMPLEADO", "TOTAL VENTAS", "NRO TRANSACCIONES", true);

        Map<String, BigDecimal> map = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (Venta v : ventas) {
            String nombre = v.getNombreEmpleado() != null ? v.getNombreEmpleado() : "(Sin asignar)";
            BigDecimal total = v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
            map.put(nombre, map.getOrDefault(nombre, BigDecimal.ZERO).add(total));
            count.put(nombre, count.getOrDefault(nombre, 0) + 1);
        }

        // ordenar por total desc
        java.util.List<Map.Entry<String, BigDecimal>> orden = new ArrayList<>(map.entrySet());
        orden.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, BigDecimal> e : orden) {
            String nombre = e.getKey();
            String total = formatoCurrency(e.getValue());
            String nro = String.valueOf(count.getOrDefault(nombre, 0));
            agregarFilaTabla(tabla, nombre, total, nro, false);
        }

        document.add(tabla);
    }

    private com.lowagie.text.Image crearGraficoVentasPorEmpleado(java.util.List<Venta> ventas, int width, int height) {
        try {
            Map<String, BigDecimal> map = new HashMap<>();
            for (Venta v : ventas) {
                String nombre = v.getNombreEmpleado() != null ? v.getNombreEmpleado() : "(Sin asignar)";
                BigDecimal total = v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
                map.put(nombre, map.getOrDefault(nombre, BigDecimal.ZERO).add(total));
            }

            if (map.isEmpty()) return null;

            // ordenar y limitar a top 8
            java.util.List<Map.Entry<String, BigDecimal>> orden = new ArrayList<>(map.entrySet());
            orden.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            int top = Math.min(8, orden.size());

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            // Mejoras de renderizado para salida profesional
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);

            int padding = 40;
            int chartWidth = width - padding * 2;
            int chartHeight = height - padding * 2;

            BigDecimal max = orden.stream().limit(top).map(Map.Entry::getValue).max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
            int barWidth = Math.max(10, chartWidth / (top * 2));
            int gap = Math.max(8, barWidth / 4);

            int x = padding + 20;
            int yBase = padding + chartHeight;

            // Dibujar líneas de grilla horizontales sutiles
            g.setColor(new java.awt.Color(230, 230, 230));
            int gridLines = 4;
            for (int i = 0; i <= gridLines; i++) {
                int gy = padding + (chartHeight * i) / gridLines;
                g.drawLine(padding, gy, padding + chartWidth, gy);
            }

            java.awt.Font labelFont = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11);
            java.awt.Font valueFont = new java.awt.Font("Helvetica", java.awt.Font.BOLD, 11);
            g.setFont(labelFont);

            for (int i = 0; i < top; i++) {
                Map.Entry<String, BigDecimal> e = orden.get(i);
                double val = e.getValue().doubleValue();
                double maxd = max.doubleValue();
                int barH = (int) ((val / (maxd == 0 ? 1 : maxd)) * (chartHeight - 40));
                // Barra en tonos de gris oscuro
                g.setColor(new java.awt.Color(60, 60, 60));
                g.fillRect(x, yBase - barH, barWidth, barH);
                // Etiqueta de valor arriba de la barra
                g.setFont(valueFont);
                g.setColor(new java.awt.Color(40, 40, 40));
                String valStr = formatoCurrency(e.getValue());
                java.awt.FontMetrics fmVal = g.getFontMetrics();
                int valW = fmVal.stringWidth(valStr);
                g.drawString(valStr, x + (barWidth - valW) / 2, Math.max(padding + 12, yBase - barH - 6));
                // Etiqueta del eje X
                g.setFont(labelFont);
                g.setColor(new java.awt.Color(80, 80, 80));
                String label = e.getKey();
                java.awt.FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                int labelX = x + (barWidth - labelWidth) / 2;
                // Si la etiqueta es muy larga, recortarla
                if (labelWidth > barWidth + 20) {
                    while (label.length() > 3 && fm.stringWidth(label + "...") > barWidth + 10) {
                        label = label.substring(0, label.length() - 1);
                    }
                    label = label + "...";
                    labelWidth = fm.stringWidth(label);
                    labelX = x + (barWidth - labelWidth) / 2;
                }
                g.drawString(label, labelX, yBase + 15);

                x += barWidth + gap;
            }

            g.dispose();
            return Image.getInstance(img, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // ============= MÉTODOS AUXILIARES =============

    private java.util.List<Venta> obtenerVentasFiltradas(String filtroProducto, String filtroEmpleado, String filtroFecha) {
        java.util.List<Venta> ventas = ventaRepository.findAll();

        if (filtroEmpleado != null && !filtroEmpleado.isEmpty()) {
            ventas = ventas.stream()
                .filter(v -> v.getNombreEmpleado() != null && 
                           v.getNombreEmpleado().toLowerCase().contains(filtroEmpleado.toLowerCase()))
                .collect(Collectors.toList());
        }

        if (filtroFecha != null && !filtroFecha.isEmpty()) {
            ventas = ventas.stream()
                .filter(v -> v.getFecha().toLocalDate().toString().startsWith(filtroFecha))
                .collect(Collectors.toList());
        }

        return ventas;
    }

    private BigDecimal calcularPromedioTicket(java.util.List<Venta> ventas) {
        if (ventas.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = ventas.stream()
            .map(Venta::getTotalVenta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(new BigDecimal(ventas.size()), 2, RoundingMode.HALF_UP);
    }

    

    private void agregarFilaTabla(Table tabla, String col1, String col2, boolean esEncabezado) 
        throws BadElementException {

        if (esEncabezado) {
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Paragraph p1 = new Paragraph(col1, headerFont);
            Paragraph p2 = new Paragraph(col2, headerFont);

            Cell cell1 = new Cell(p1);
            Cell cell2 = new Cell(p2);
                cell1.setBackgroundColor(new Color(60, 60, 60));
                cell2.setBackgroundColor(new Color(60, 60, 60));
            

            tabla.addCell(cell1);
            tabla.addCell(cell2);
        } else {
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Cell cell1 = new Cell(new Paragraph(col1, normalFont));
            Cell cell2 = new Cell(new Paragraph(col2, normalFont));
            tabla.addCell(cell1);
            tabla.addCell(cell2);
        }
    }

    private void agregarFilaTabla(Table tabla, String col1, String col2, String col3, boolean esEncabezado) 
        throws BadElementException {

        if (esEncabezado) {
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Paragraph p1 = new Paragraph(col1, headerFont);
            Paragraph p2 = new Paragraph(col2, headerFont);
            Paragraph p3 = new Paragraph(col3, headerFont);

            Cell cell1 = new Cell(p1);
            Cell cell2 = new Cell(p2);
            Cell cell3 = new Cell(p3);
            cell1.setBackgroundColor(new Color(60, 60, 60));
            cell2.setBackgroundColor(new Color(60, 60, 60));
            cell3.setBackgroundColor(new Color(60, 60, 60));

            tabla.addCell(cell1);
            tabla.addCell(cell2);
            tabla.addCell(cell3);
        } else {
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Cell cell1 = new Cell(new Paragraph(col1, normalFont));
            Cell cell2 = new Cell(new Paragraph(col2, normalFont));
            Cell cell3 = new Cell(new Paragraph(col3, normalFont));
            tabla.addCell(cell1);
            tabla.addCell(cell2);
            tabla.addCell(cell3);
        }
    }

    private String formatoCurrency(BigDecimal valor) {
        return "$ " + String.format("%,.2f", valor);
    }
}
