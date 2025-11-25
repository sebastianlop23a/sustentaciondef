package SCRUM3.Bj_Byte.util;

import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.VentaDetalle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;

public class ExportarExcelVentas {

    private final List<Venta> listaVentas;
    private final XSSFWorkbook workbook;
    private final Sheet sheet;

    public ExportarExcelVentas(List<Venta> listaVentas) {
        this.listaVentas = listaVentas;
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Ventas");
    }

    private void escribirCabecera() {
        Row fila = sheet.createRow(0);
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);

        String[] encabezados = { "ID", "Empleado", "Producto", "Cantidad", "Total", "Fecha" };

        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estilo);
        }
    }

    private void escribirDatos() {
        int filaNum = 1;
        for (Venta venta : listaVentas) {
            if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
                Row fila = sheet.createRow(filaNum++);
                fila.createCell(0).setCellValue(venta.getId());
                fila.createCell(1).setCellValue(venta.getNombreEmpleado() != null ? venta.getNombreEmpleado() : "N/A");
                fila.createCell(2).setCellValue("N/A");
                fila.createCell(3).setCellValue(0);
                double totalRedondeado = venta.getTotalVenta() != null ?
                        venta.getTotalVenta().setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
                fila.createCell(4).setCellValue(totalRedondeado);
                fila.createCell(5).setCellValue(venta.getFecha() != null ? venta.getFecha().toString() : "");
                continue;
            }

            for (VentaDetalle d : venta.getDetalles()) {
                Row fila = sheet.createRow(filaNum++);
                fila.createCell(0).setCellValue(venta.getId());
                fila.createCell(1).setCellValue(venta.getNombreEmpleado() != null ? venta.getNombreEmpleado() : "N/A");

                String prod = "N/A";
                double subtotal = 0.0;
                if (d.getInventario() != null && d.getInventario().getProducto() != null) {
                    prod = d.getInventario().getProducto().getNombre();
                    if (d.getInventario().getProducto().getPrecio() != null) {
                        subtotal = d.getInventario().getProducto().getPrecio()
                                .multiply(java.math.BigDecimal.valueOf(d.getCantidad()))
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                }

                fila.createCell(2).setCellValue(prod);
                fila.createCell(3).setCellValue(d.getCantidad());
                fila.createCell(4).setCellValue(subtotal);
                fila.createCell(5).setCellValue(venta.getFecha() != null ? venta.getFecha().toString() : "");
            }
        }

        // Autoajustar tamaño de columnas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void exportar(HttpServletResponse response) throws IOException {
        escribirCabecera();
        escribirDatos();

        try (ServletOutputStream out = response.getOutputStream()) {
            workbook.write(out);
            workbook.close();
        }
    }
}
