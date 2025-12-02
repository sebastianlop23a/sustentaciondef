package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.service.InformeFinancieroService;
import SCRUM3.Bj_Byte.service.dto.GenerarInformeFinancieroRequest;
import SCRUM3.Bj_Byte.service.exception.InformeFinancieroException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestionar la generación de informes financieros.
 *
 * Responsabilidades:
 * - Recibir solicitudes HTTP de informe
 * - Validar parámetros de entrada
 * - Delegar generación a InformeFinancieroService
 * - Manejar errores y retornar respuestas HTTP apropiadas
 *
 * Ejemplo de uso:
 * POST /api/informes/financiero
 * Content-Type: application/json
 * {
 *   "productoFiltro": null,
 *   "empleadoFiltro": "JUAN",
 *   "fechaFiltro": "2024-01-01",
 *   "tipo": "PDF",
 *   "metadatos": { "incluirGraficos": true }
 * }
 */
@RestController
@RequestMapping("/api/informes")
public class InformeFinancieroController {

    private static final Logger logger = LoggerFactory.getLogger(InformeFinancieroController.class);
    private final InformeFinancieroService informeService;

    @Autowired
    public InformeFinancieroController(InformeFinancieroService informeService) {
        this.informeService = informeService;
    }

    /**
     * Endpoint para generar informe financiero en PDF.
     *
     * Parámetros Query (opcionales):
     * - productoFiltro: Filtrar por nombre de producto
     * - empleadoFiltro: Filtrar por nombre de empleado
     * - fechaFiltro: Filtrar por fecha (yyyy-MM-dd)
     *
     * @param productoFiltro Nombre del producto (opcional)
     * @param empleadoFiltro Nombre del empleado (opcional)
     * @param fechaFiltro Fecha de filtro (opcional)
     * @param response Objeto HttpServletResponse para escribir el PDF
     * @return void (el PDF se escribe directamente en response)
     */
    @GetMapping("/financiero")
    public void generarInformePdf(
            @RequestParam(required = false) String productoFiltro,
            @RequestParam(required = false) String empleadoFiltro,
            @RequestParam(required = false) String fechaFiltro,
            HttpServletResponse response) {
        try {
            logger.info("Solicitud de informe financiero - Filtros: producto={}, empleado={}, fecha={}",
                    productoFiltro, empleadoFiltro, fechaFiltro);

            GenerarInformeFinancieroRequest request = new GenerarInformeFinancieroRequest(
                    productoFiltro,
                    empleadoFiltro,
                    fechaFiltro,
                    "PDF",
                    new java.util.HashMap<>()
            );

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=informe-financiero.pdf");

            informeService.generarInforme(request, response);

        } catch (InformeFinancieroException e) {
            logger.error("Error de negocio al generar informe: {} - {}", e.getCodigo(), e.getDetalles());
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                logger.error("Error al escribir respuesta de error: {}", ex.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error inesperado al generar informe: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Error interno al generar el informe\"}");
            } catch (Exception ex) {
                logger.error("Error al escribir respuesta de error: {}", ex.getMessage());
            }
        }
    }

    /**
     * Endpoint para validar disponibilidad del servicio de informes.
     *
     * @return Estado del servicio
     */
    @GetMapping("/financiero/status")
    public ResponseEntity<?> statusInforme() {
        try {
            logger.debug("Verificando estado del servicio de informes");
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("servicio", "InformeFinanciero");
                put("estado", "disponible");
                put("timestamp", java.time.LocalDateTime.now());
            }});
        } catch (Exception e) {
            logger.error("Error al obtener estado del servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new java.util.HashMap<String, String>() {{
                        put("error", "Servicio no disponible");
                    }});
        }
    }

    /**
     * Endpoint alternativo: POST para solicitudes complejas.
     *
     * @param request DTO con parámetros de solicitud
     * @param response Objeto HttpServletResponse para escribir el PDF
     * @return void
     */
    @PostMapping("/financiero")
    public void generarInformePost(
            @RequestBody GenerarInformeFinancieroRequest request,
            HttpServletResponse response) {
        try {
            logger.info("Solicitud POST de informe financiero - Tipo: {}", request.getTipo());

            if (!request.esValido()) {
                throw new InformeFinancieroException(
                        "SOLICITUD_INVALIDA",
                        "La solicitud no cumple requisitos mínimos",
                        null
                );
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=informe-financiero.pdf");

            informeService.generarInforme(request, response);

        } catch (InformeFinancieroException e) {
            logger.error("Error de negocio al generar informe: {} - {}", e.getCodigo(), e.getDetalles());
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                logger.error("Error al escribir respuesta de error: {}", ex.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error inesperado al generar informe: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Error interno al generar el informe\"}");
            } catch (Exception ex) {
                logger.error("Error al escribir respuesta de error: {}", ex.getMessage());
            }
        }
    }
}
