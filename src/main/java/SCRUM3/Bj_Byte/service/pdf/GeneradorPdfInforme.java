package SCRUM3.Bj_Byte.service.pdf;

import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.service.dto.MetricasFinancieras;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Interfaz que define el contrato para la generación de documentos PDF.
 * Separa la lógica de generación de PDF de la lógica de negocio.
 * Implementa el patrón Strategy permitiendo múltiples generadores.
 */
public interface GeneradorPdfInforme {

    /**
     * Genera un documento PDF con el informe financiero y lo escribe en la respuesta HTTP.
     *
     * @param response Respuesta HTTP donde se escribirá el PDF
     * @param metricas Métricas financieras a incluir en el informe
     * @param ventas Lista de ventas a detalle
     * @param ventasPorEmpleado Mapa de ventas agrupadas por empleado
     * @param distribucionArticulos Mapa de distribución de artículos
     * @throws Exception Si ocurre un error durante la generación del PDF
     */
    void generarInforme(HttpServletResponse response,
                        MetricasFinancieras metricas,
                        List<Venta> ventas,
                        Map<String, java.math.BigDecimal> ventasPorEmpleado,
                        Map<String, Integer> distribucionArticulos) throws Exception;
}
