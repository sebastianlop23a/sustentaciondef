package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.factory.ProductoFactory;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashSet;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Transactional
    public void cargarProductosDesdeCSV(InputStream inputStream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String linea;
            boolean esPrimeraLinea = true;
            int contador = 0;
            int contadorExitosos = 0;
            int formatoColumnas = 0; // 0=desconocido, 4=antiguo, 6=nuevo, 8=completo

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] campos = linea.split(",");

                // Detectar formato en la primera línea válida
                if (esPrimeraLinea) {
                    esPrimeraLinea = false;
                    
                    // Si es header, ignorar y continuar
                    if (campos[0].trim().toLowerCase().contains("id") || 
                        campos[0].trim().toLowerCase().contains("nombre")) {
                        // Detectar formato por el header
                        if (campos.length == 4) {
                            formatoColumnas = 4;
                            System.out.println("✓ Detectado formato de 4 columnas: id, nombre, descripcion, precio");
                        } else if (campos.length == 6) {
                            formatoColumnas = 6;
                            System.out.println("✓ Detectado formato de 6 columnas: id, nombre, proveedores, descripcion, precio, precio_base");
                        } else if (campos.length == 8) {
                            formatoColumnas = 8;
                            System.out.println("✓ Detectado formato de 8 columnas: nombre, descripcion, precio, precio_base, ganancia, cvv, exento, activo");
                        }
                        continue;
                    } else {
                        // Primera línea es datos, no header. Detectar por cantidad de columnas
                        if (campos.length == 4) {
                            formatoColumnas = 4;
                            System.out.println("✓ Detectado formato de 4 columnas (sin header)");
                        } else if (campos.length == 6) {
                            formatoColumnas = 6;
                            System.out.println("✓ Detectado formato de 6 columnas (sin header)");
                        } else if (campos.length == 8) {
                            formatoColumnas = 8;
                            System.out.println("✓ Detectado formato de 8 columnas (sin header)");
                        } else {
                            System.err.println("ERROR: Formato de CSV no reconocido. Esperaba 4, 6 u 8 columnas, recibió " + campos.length);
                            throw new RuntimeException("Formato de CSV no válido. Debe tener 4, 6 u 8 columnas.");
                        }
                        esPrimeraLinea = false; // Ya procesamos la primera línea de datos
                    }
                }

                // Si aún no detectamos formato, algo está mal
                if (formatoColumnas == 0) {
                    if (campos.length == 4) {
                        formatoColumnas = 4;
                    } else if (campos.length == 6) {
                        formatoColumnas = 6;
                    } else if (campos.length == 8) {
                        formatoColumnas = 8;
                    }
                }

                // Validar columnas
                if (campos.length != formatoColumnas) {
                    System.err.println("Línea " + (contador + 1) + " inválida (esperaba " + formatoColumnas + " columnas, recibió " + campos.length + "): " + linea);
                    contador++;
                    continue;
                }

                try {
                    Producto producto = null;

                    if (formatoColumnas == 4) {
                        // Formato antiguo: 0=id, 1=nombre, 2=descripcion, 3=precio
                        String nombre = campos[1].trim();
                        String descripcion = campos[2].trim();
                        String precioStr = campos[3].trim();

                        if (nombre.isEmpty() || precioStr.isEmpty()) {
                            System.err.println("Línea " + (contador + 1) + " ignorada: Nombre o Precio vacío");
                            contador++;
                            continue;
                        }

                        BigDecimal precio = parseBigDecimal(precioStr);
                        BigDecimal precioBase = precio;
                        BigDecimal ganancia = BigDecimal.ZERO;

                        producto = ProductoFactory.crearProductoConPrecioBase(nombre, descripcion, precio, precioBase);
                        producto.setGanancia(ganancia);
                        producto.setCvv(null);
                        producto.setExento(false);
                        producto.setActivo(true);

                    } else if (formatoColumnas == 6) {
                        // Formato 6 columnas: 0=id, 1=nombre, 2=proveedores, 3=descripcion, 4=precio, 5=precio_base
                        String nombre = campos[1].trim();
                        // campos[2] = proveedores (se ignora por ahora)
                        String descripcion = campos[3].trim();
                        String precioStr = campos[4].trim();
                        String precioBaseStr = campos[5].trim();

                        if (nombre.isEmpty() || precioStr.isEmpty() || precioBaseStr.isEmpty()) {
                            System.err.println("Línea " + (contador + 1) + " ignorada: Nombre, Precio o PrecioBase vacío");
                            contador++;
                            continue;
                        }

                        BigDecimal precio = parseBigDecimal(precioStr);
                        BigDecimal precioBase = parseBigDecimal(precioBaseStr);
                        BigDecimal ganancia = precio.subtract(precioBase);

                        producto = ProductoFactory.crearProductoConPrecioBase(nombre, descripcion, precio, precioBase);
                        producto.setGanancia(ganancia);
                        producto.setCvv(null);
                        producto.setExento(false);
                        producto.setActivo(true);

                    } else if (formatoColumnas == 8) {
                        // Formato completo: 0=nombre, 1=descripcion, 2=precio, 3=precio_base, 4=ganancia, 5=cvv, 6=exento, 7=activo
                        String nombre = campos[0].trim();
                        String descripcion = campos[1].trim();
                        String precioStr = campos[2].trim();
                        String precioBaseStr = campos[3].trim();
                        String gananciaStr = campos[4].trim();
                        String cvv = campos[5].trim();
                        String exentoStr = campos[6].trim();
                        String activoStr = campos[7].trim();

                        if (nombre.isEmpty() || precioStr.isEmpty() || precioBaseStr.isEmpty()) {
                            System.err.println("Línea " + (contador + 1) + " ignorada: Nombre, Precio o PrecioBase vacío");
                            contador++;
                            continue;
                        }

                        BigDecimal precio = parseBigDecimal(precioStr);
                        BigDecimal precioBase = parseBigDecimal(precioBaseStr);
                        BigDecimal ganancia = gananciaStr.isEmpty() ? precio.subtract(precioBase) : parseBigDecimal(gananciaStr);

                        Boolean exento = parseBoolean(exentoStr);
                        Boolean activo = parseBoolean(activoStr);

                        producto = ProductoFactory.crearProductoConPrecioBase(nombre, descripcion, precio, precioBase);
                        producto.setGanancia(ganancia);
                        producto.setCvv(cvv.isEmpty() ? null : cvv);
                        producto.setExento(exento);
                        producto.setActivo(activo);
                    }

                    if (producto != null) {
                        producto.setProveedores(new HashSet<>());

                        System.out.println("ANTES DE GUARDAR: " + producto.getNombre() + 
                            " | Precio: " + producto.getPrecio() + 
                            " | PrecioBase: " + producto.getPrecioBase() +
                            " | Ganancia: " + producto.getGanancia() +
                            " | Exento: " + producto.getExento() +
                            " | Activo: " + producto.getActivo());

                        Producto guardado = productoRepository.save(producto);
                        
                        System.out.println("✓ GUARDADO EN BD CON ID: " + guardado.getId() + 
                            " | Nombre: " + guardado.getNombre() + 
                            " | Precio: " + guardado.getPrecio());
                        contadorExitosos++;
                    }
                    contador++;

                } catch (NumberFormatException e) {
                    System.err.println("Error parseando valores numéricos en línea " + (contador + 1) + ": " + linea);
                    System.err.println("Detalle: " + e.getMessage());
                    contador++;
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + (contador + 1) + ": " + linea);
                    System.err.println("Detalle: " + e.getMessage());
                    contador++;
                }
            }

            System.out.println("\n========== RESUMEN DE CARGA ==========");
            System.out.println("Formato detectado: " + formatoColumnas + " columnas");
            System.out.println("Total líneas procesadas: " + contador);
            System.out.println("Productos cargados exitosamente: " + contadorExitosos);
            System.out.println("=====================================\n");
            
            if (contadorExitosos == 0) {
                throw new RuntimeException("No se cargaron productos. Verifique el formato del CSV.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al procesar CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea un String a BigDecimal, soportando formatos con . o , como separador decimal
     */
    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Remover espacios
        valor = valor.trim();
        
        // Si contiene punto y coma, asumir formato europeo (1.234,56)
        if (valor.contains(".") && valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        } 
        // Si solo contiene coma, asumir formato europeo (1234,56)
        else if (valor.contains(",")) {
            valor = valor.replace(",", ".");
        }
        
        return new BigDecimal(valor);
    }

    /**
     * Parsea un String a Boolean, aceptando true/false, si/no, etc
     */
    private Boolean parseBoolean(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return false;
        }
        
        String lower = valor.trim().toLowerCase();
        return lower.equals("true") || lower.equals("si") || lower.equals("sí") || 
               lower.equals("1") || lower.equals("yes") || lower.equals("y");
    }
}
