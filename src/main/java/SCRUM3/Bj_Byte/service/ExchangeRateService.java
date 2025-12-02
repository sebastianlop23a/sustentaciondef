package SCRUM3.Bj_Byte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/COP";
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);
    
    // Caché en memoria de tasas: moneda -> tasa
    private final Map<String, BigDecimal> cachedRates = new ConcurrentHashMap<>();
    private long lastUpdateTimestamp = 0;
    
    // Tasas por defecto (fallback) aproximadas en COP
    private static final Map<String, Double> DEFAULT_RATES = Map.of(
        "USD", 0.00025,  // 1 COP ≈ 0.00025 USD
        "EUR", 0.00023   // 1 COP ≈ 0.00023 EUR
    );

    public ExchangeRateService(RestTemplateBuilder builder) {
        // Configurar RestTemplate con timeouts explícitos
        this.restTemplate = builder
            .setConnectTimeout(java.time.Duration.ofSeconds(5))
            .setReadTimeout(java.time.Duration.ofSeconds(5))
            .requestFactory(this::clientHttpRequestFactory)
            .build();
        
        // Inicializar caché con tasas por defecto
        DEFAULT_RATES.forEach((currency, rate) -> 
            cachedRates.put(currency, BigDecimal.valueOf(rate))
        );
    }
    
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 segundos
        factory.setReadTimeout(5000);    // 5 segundos
        return factory;
    }
    
    /**
     * Scheduler: intenta actualizar tasas cada hora en background (sin bloquear)
     */
    @Scheduled(fixedRate = 3600000) // 1 hora = 3600000 ms
    public void actualizarTasasEnBackground() {
        logger.info("Iniciando actualización de tasas de cambio en background...");
        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    API_URL,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> response = responseEntity.getBody();
            if (response != null) {
                Object ratesObj = response.get("rates");
                if (ratesObj instanceof Map) {
                    Map<?, ?> rates = (Map<?, ?>) ratesObj;
                    
                    // Actualizar caché con tasas obtenidas
                    for (String currency : new String[]{"USD", "EUR"}) {
                        if (rates.containsKey(currency)) {
                            try {
                                BigDecimal rateBD = toBigDecimal(rates.get(currency));
                                cachedRates.put(currency, rateBD);
                                logger.info("✅ Tasa {} actualizada: {}", currency, rateBD);
                            } catch (NumberFormatException nfe) {
                                logger.warn("⚠️ No se pudo parsear tasa {}: {}", currency, rates.get(currency));
                            }
                        }
                    }
                    
                    lastUpdateTimestamp = System.currentTimeMillis();
                    logger.info("✅ Tasas de cambio actualizadas exitosamente");
                } else {
                    logger.warn("Formato inesperado de rates: {}", ratesObj);
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo actualizar tasas en background: {}", e.getMessage());
            // Las tasas en caché se mantienen, no hay problema
        }
    }

    /**
     * Convierte desde COP a otra moneda (ej: USD, EUR).
     * Usa el caché. Si no existe, usa tasas por defecto.
     * Las tasas se actualizan cada hora en background.
     */
    public BigDecimal convertFromCOP(BigDecimal amount, String targetCurrency) {
        if (amount == null || targetCurrency == null || targetCurrency.isBlank()) {
            return BigDecimal.ZERO;
        }
        
        // Intentar usar caché
        BigDecimal rate = cachedRates.get(targetCurrency);
        if (rate != null) {
            logger.debug("Usando tasa en caché para {}: {}", targetCurrency, rate);
            return amount.multiply(rate);
        }
        
        // Fallback: usar tasas por defecto
        Double defaultRate = DEFAULT_RATES.get(targetCurrency);
        if (defaultRate != null) {
            logger.warn("Tasa no en caché, usando tasa por defecto para {}: {}", targetCurrency, defaultRate);
            return amount.multiply(BigDecimal.valueOf(defaultRate));
        }
        
        return BigDecimal.ZERO; // En caso de error completo
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof Number) {
            return new BigDecimal(val.toString());
        } else if (val instanceof String) {
            return new BigDecimal((String) val);
        }
        throw new NumberFormatException("Valor de tasa no convertible: " + val);
    }

    /**
     * Obtiene la fecha de la última actualización en español.
     * Devuelve la fecha actual (desde la última actualización del caché).
     */
    public String getUltimaActualizacion() {
        SimpleDateFormat formatoEspanol = new SimpleDateFormat("EEEE, d 'de' MMMM yyyy HH:mm:ss", Locale.forLanguageTag("es-ES"));
        
        if (lastUpdateTimestamp > 0) {
            return formatoEspanol.format(new Date(lastUpdateTimestamp));
        }
        
        // Si nunca se actualizó, devolver fecha actual
        return formatoEspanol.format(new Date());
    }
    
    /**
     * Obtiene el estado actual del caché (tasas y última actualización).
     * Útil para debugging o endpoints REST.
     */
    public Map<String, Object> obtenerEstadoCache() {
        return Map.of(
            "tasas", new HashMap<>(cachedRates),
            "ultimaActualizacion", lastUpdateTimestamp > 0 
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.forLanguageTag("es-ES")).format(new Date(lastUpdateTimestamp))
                : "No actualizado",
            "estado", cachedRates.isEmpty() ? "SIN CACHÉ" : "CON CACHÉ"
        );
    }
}
