package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.VentaDetalle;
import SCRUM3.Bj_Byte.repository.VentaRepository;
import SCRUM3.Bj_Byte.service.dto.GenerarInformeFinancieroRequest;
import SCRUM3.Bj_Byte.service.dto.MetricasFinancieras;
import SCRUM3.Bj_Byte.service.exception.InformeFinancieroException;
import SCRUM3.Bj_Byte.service.pdf.GeneradorPdfInforme;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RESUMEN EJECUTIVO:
 * Servicio centralizado para la generación de informes financieros en formato PDF.
 * Encapsula la lógica de cálculo de métricas, filtrado de datos y delegación de
 * renderización. Sigue principios SOLID y Clean Architecture.
 *
 * DESCRIPCION TECNICA:
 * Propósito: Gestionar el ciclo de vida completo de generación de informes financieros.
 * Entradas: Criterios de filtro (producto, empleado, fecha), respuesta HTTP para salida.
 * Salidas: Documento PDF con métricas, gráficos y tablas.
 * Flujo Interno:
 *   1. Validar entrada
 *   2. Obtener y filtrar datos de ventas
 *   3. Calcular métricas financieras
 *   4. Agrupar datos para visualizaciones
 *   5. Delegar renderización al GeneradorPdfInforme
 *   6. Manejar errores de forma centralizada
 *
 * ESTRUCTURA:
 * - Inyección de dependencias: VentaRepository, GeneradorPdfInforme
 * - Métodos privados especializados para cada responsabilidad
 * - Manejo centralizado de excepciones
 * - DTOs para desacoplamiento entre capas
 *
 * PRINCIPIOS APLICADOS:
 * - S (Single Responsibility): Cada método tiene una responsabilidad clara
 * - O (Open/Closed): Extensible sin modificar; usa interfaces para generador
 * - L (Liskov Substitution): GeneradorPdfInforme permite múltiples implementaciones
 * - I (Interface Segregation): Interfaces pequeñas y específicas
 * - D (Dependency Inversion): Depende de abstracciones, no de implementaciones
 */
@Service
public class InformeFinancieroService {

    private static final Logger logger = LoggerFactory.getLogger(InformeFinancieroService.class);
    private static final String CODIGO_ERROR_DATOS = "INFORME_DATOS_VACIO";
    private static final String CODIGO_ERROR_GENERACION = "INFORME_GENERACION_FALLO";
    private static final int ESCALA_DECIMAL = 2;

    private final VentaRepository ventaRepository;
    private final GeneradorPdfInforme generadorPdf;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param ventaRepository Repositorio de acceso a datos de ventas
     * @param generadorPdf Implementación del generador de PDF
     */
    @Autowired
    public InformeFinancieroService(VentaRepository ventaRepository,
                                    GeneradorPdfInforme generadorPdf) {
        this.ventaRepository = ventaRepository;
        this.generadorPdf = generadorPdf;
    }

    /**
     * Genera un informe financiero basado en criterios de filtro.
     * Orquesta el flujo de obtención de datos, cálculo de métricas y generación de PDF.
     *
     * @param request Objeto con criterios de filtro (producto, empleado, fecha)
     * @param response Respuesta HTTP para escribir el PDF
     * @throws InformeFinancieroException Si los datos son insuficientes o la generación falla
     */
    public void generarInforme(GenerarInformeFinancieroRequest request,
                               HttpServletResponse response) {
        try {
            logger.info("Iniciando generacion de informe financiero");

            validarEntrada(request);

            List<Venta> ventasFiltradas = obtenerYFiltrarVentas(request);

            if (ventasFiltradas.isEmpty()) {
                logger.warn("No se encontraron ventas con los filtros especificados");
                throw new InformeFinancieroException(
                        CODIGO_ERROR_DATOS,
                        "No hay datos disponibles para generar el informe con los filtros especificados"
                );
            }

            MetricasFinancieras metricas = calcularMetricas(ventasFiltradas);

            Map<String, BigDecimal> ventasPorEmpleado = agruparVentasPorEmpleado(ventasFiltradas);
            Map<String, Integer> distribucionArticulos = agruparDistribucionArticulos(ventasFiltradas);

            configuraRespuestaHttp(response);
            generadorPdf.generarInforme(response, metricas, ventasFiltradas,
                    ventasPorEmpleado, distribucionArticulos);

            logger.info("Informe financiero generado exitosamente");

        } catch (InformeFinancieroException e) {
            logger.error("Error de negocio en generacion de informe: {} - {}", e.getCodigo(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en generacion de informe", e);
            throw new InformeFinancieroException(
                    CODIGO_ERROR_GENERACION,
                    "Error al generar el informe financiero",
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Valida que los parámetros de entrada sean correctos.
     * Realiza validaciones básicas de negocio.
     *
     * @param request Objeto con criterios de filtro
     * @throws InformeFinancieroException Si la validación falla
     */
    private void validarEntrada(GenerarInformeFinancieroRequest request) {
        if (request == null || !request.esValido()) {
            throw new InformeFinancieroException(
                    "INFORME_ENTRADA_INVALIDA",
                    "Los parámetros de entrada son inválidos"
            );
        }
    }

    /**
     * Obtiene todas las ventas y aplica filtros según criterios.
     * Implementa lógica funcional para filtrado eficiente.
     *
     * @param request Objeto con criterios de filtro
     * @return Lista de ventas filtradas
     */
    private List<Venta> obtenerYFiltrarVentas(GenerarInformeFinancieroRequest request) {
        List<Venta> todasLasVentas = ventaRepository.findAll();

        return todasLasVentas.stream()
                .filter(v -> cumpleFiltroProducto(v, request.getProductoFiltro()))
                .filter(v -> cumpleFiltroEmpleado(v, request.getEmpleadoFiltro()))
                .filter(v -> cumpleFiltroFecha(v, request.getFechaFiltro()))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si una venta cumple con el filtro de producto.
     *
     * @param venta Venta a evaluar
     * @param productoFiltro Filtro de producto (null o vacío = sin filtro)
     * @return true si cumple o no hay filtro
     */
    private boolean cumpleFiltroProducto(Venta venta, String productoFiltro) {
        if (productoFiltro == null || productoFiltro.isEmpty()) {
            return true;
        }
        return venta.getDetalles().stream()
                .anyMatch(d -> d.getInventario() != null &&
                        d.getInventario().getProducto() != null &&
                        d.getInventario().getProducto().getNombre().equals(productoFiltro));
    }

    /**
     * Verifica si una venta cumple con el filtro de empleado.
     *
     * @param venta Venta a evaluar
     * @param empleadoFiltro Filtro de empleado (null o vacío = sin filtro)
     * @return true si cumple o no hay filtro
     */
    private boolean cumpleFiltroEmpleado(Venta venta, String empleadoFiltro) {
        if (empleadoFiltro == null || empleadoFiltro.isEmpty()) {
            return true;
        }
        return venta.getNombreEmpleado() != null &&
                venta.getNombreEmpleado().equals(empleadoFiltro);
    }

    /**
     * Verifica si una venta cumple con el filtro de fecha.
     *
     * @param venta Venta a evaluar
     * @param fechaFiltro Filtro de fecha (null = sin filtro)
     * @return true si cumple o no hay filtro
     */
    private boolean cumpleFiltroFecha(Venta venta, java.time.LocalDate fechaFiltro) {
        if (fechaFiltro == null) {
            return true;
        }
        return venta.getFecha() != null &&
                venta.getFecha().toLocalDate().equals(fechaFiltro);
    }

    /**
     * Calcula las métricas financieras principales a partir de las ventas.
     *
     * @param ventas Lista de ventas
     * @return Objeto con métricas calculadas
     */
    private MetricasFinancieras calcularMetricas(List<Venta> ventas) {
        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotalVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalArticulos = ventas.stream()
                .mapToInt(v -> v.getDetalles().stream()
                        .mapToInt(VentaDetalle::getCantidad)
                        .sum())
                .sum();

        BigDecimal promedioVenta = ventas.size() > 0
                ? totalVentas.divide(new BigDecimal(ventas.size()), ESCALA_DECIMAL, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MetricasFinancieras(
                totalVentas,
                totalArticulos,
                ventas.size(),
                promedioVenta,
                LocalDateTime.now()
        );
    }

    /**
     * Agrupa las ventas por nombre de empleado.
     *
     * @param ventas Lista de ventas
     * @return Mapa con totales por empleado
     */
    private Map<String, BigDecimal> agruparVentasPorEmpleado(List<Venta> ventas) {
        return ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getNombreEmpleado() != null ? v.getNombreEmpleado() : "Sin asignar",
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Venta::getTotalVenta,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * Agrupa la cantidad de artículos vendidos por producto.
     *
     * @param ventas Lista de ventas
     * @return Mapa con distribución de artículos por producto
     */
    private Map<String, Integer> agruparDistribucionArticulos(List<Venta> ventas) {
        Map<String, Integer> distribucion = new HashMap<>();
        for (Venta venta : ventas) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                if (detalle.getInventario() != null &&
                    detalle.getInventario().getProducto() != null) {
                    String nombreProducto = detalle.getInventario().getProducto().getNombre();
                    distribucion.put(nombreProducto,
                            distribucion.getOrDefault(nombreProducto, 0) + detalle.getCantidad());
                }
            }
        }
        return distribucion;
    }

    /**
     * Configura los headers HTTP necesarios para la descarga del PDF.
     *
     * @param response Respuesta HTTP
     */
    private void configuraRespuestaHttp(HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=informe_financiero_" + System.currentTimeMillis() + ".pdf");
    }

    /**
     * POSIBLES EXTENSIONES:
     *
     * 1. Generación asincrónica:
     *    - Usar @Async para generar PDFs en background
     *    - Almacenar en Storage (S3, Azure Blob) en lugar de response directo
     *    - Notificar via email al usuario
     *
     * 2. Cacheo de reportes:
     *    - Implementar Redis para cachear reportes por hash de criterios
     *    - Generar reportes overnight para acceso inmediato
     *
     * 3. Múltiples formatos:
     *    - Crear GeneradorExcel, GeneradorCSV implementando la interfaz
     *    - Patrón Factory para seleccionar generador por tipo
     *
     * 4. Análisis avanzado:
     *    - Agregar predicciones de tendencias usando ML
     *    - Comparativas con períodos anteriores
     *    - Alertas automáticas por anomalías
     *
     * 5. Internacionalización:
     *    - Soportar múltiples idiomas y formatos de moneda
     *    - Usar MessageSource para i18n
     *
     * 6. Auditoría:
     *    - Loguear cada generación en tabla de auditoría
     *    - Rastrear quién generó, cuándo y qué filtros usó
     *
     * 7. Optimizaciones de performance:
     *    - Paginación para listas muy grandes
     *    - Usar queries optimizadas con @Query personalizadas
     *    - Índices en filtros frecuentes
     *
     * EJEMPLO DE USO:
     *
     * GenerarInformeFinancieroRequest request = new GenerarInformeFinancieroRequest(
     *     null,                          // sin filtro de producto
     *     "Juan Perez",                  // solo ventas de este empleado
     *     LocalDate.of(2024, 1, 15)      // solo de esta fecha
     * );
     *
     * try {
     *     informeService.generarInforme(request, httpServletResponse);
     * } catch (InformeFinancieroException e) {
     *     logger.error("Codigo: {}, Mensaje: {}", e.getCodigo(), e.getMessage());
     *     response.sendError(HttpServletResponse.SC_BAD_REQUEST);
     * }
     */
}
