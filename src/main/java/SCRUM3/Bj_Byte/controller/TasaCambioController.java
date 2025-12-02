package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para gestionar tasas de cambio.
 * Permite consultar el estado del caché y forzar actualizaciones manuales.
 */
@RestController
@RequestMapping("/api/tasas-cambio")
public class TasaCambioController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    /**
     * GET /api/tasas-cambio/estado
     * Retorna el estado actual del caché de tasas.
     */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        return ResponseEntity.ok(exchangeRateService.obtenerEstadoCache());
    }

    /**
     * POST /api/tasas-cambio/actualizar
     * Fuerza la actualización inmediata de tasas desde la API externa.
     */
    @PostMapping("/actualizar")
    public ResponseEntity<Map<String, String>> actualizarTasas() {
        try {
            exchangeRateService.actualizarTasasEnBackground();
            return ResponseEntity.ok(Map.of(
                "estado", "éxito",
                "mensaje", "Tasas actualizadas correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "estado", "error",
                "mensaje", "Error al actualizar: " + e.getMessage()
            ));
        }
    }
}
