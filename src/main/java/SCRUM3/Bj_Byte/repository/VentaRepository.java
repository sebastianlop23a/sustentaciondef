package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // =========================================================
    // CONSULTAS DE FILTRADO PARA LA LISTA DE VENTAS 🔎
    // =========================================================

    /**
     * Obtiene una lista de todas las fechas de venta únicas en formato 'yyyy-MM-dd'.
     * Arreglado: ahora funciona con DISTINCT y ORDER BY sin error.
     */
    @Query("""
           SELECT DISTINCT FUNCTION('DATE_FORMAT', v.fecha, '%Y-%m-%d') AS fecha
           FROM Venta v
           ORDER BY fecha DESC
           """)
    List<String> findDistinctDates();

    /**
     * Filtro combinado que utiliza parámetros opcionales (producto, empleado, fecha)
     * para buscar ventas. Soporta paginación.
     */
    @Query("""
            SELECT DISTINCT v 
            FROM Venta v 
            LEFT JOIN v.detalles d
            LEFT JOIN d.inventario i
            LEFT JOIN i.producto p
            WHERE 
                (:producto IS NULL OR :producto = '' OR LOWER(p.nombre) = LOWER(:producto)) 
                AND 
                (:empleado IS NULL OR :empleado = '' OR LOWER(v.nombreEmpleado) = LOWER(:empleado))
                AND
                (:fecha IS NULL OR :fecha = '' OR FUNCTION('DATE_FORMAT', v.fecha, '%Y-%m-%d') = :fecha)
            ORDER BY v.fecha DESC
            """)
    Page<Venta> buscarVentasConFiltrosPaginadas(
            @Param("producto") String producto, 
            @Param("empleado") String empleado, 
            @Param("fecha") String fecha,
            Pageable pageable);

    // =========================================================
    // CONSULTAS DE BÚSQUEDA DIRECTA 🔍
    // =========================================================

    // Ventas realizadas por un empleado específico
    List<Venta> findByEmpleadoId(Long empleadoId);

    // Ventas realizadas entre dos fechas
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    // Filtrar ventas por producto (LIKE)
    @Query("""
            SELECT v
            FROM Venta v
            JOIN v.detalles d
            JOIN d.inventario i
            JOIN i.producto p
            WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreProducto, '%'))
            """)
    List<Venta> buscarPorProducto(@Param("nombreProducto") String nombreProducto);

    // Filtrar ventas por cliente
    @Query("SELECT v FROM Venta v WHERE LOWER(v.cliente) LIKE LOWER(CONCAT('%', :cliente, '%'))")
    List<Venta> buscarPorCliente(@Param("cliente") String cliente);
    
    // Filtrar ventas por método de pago
    @Query("SELECT v FROM Venta v WHERE v.metodoPago = :metodoPago")
    List<Venta> buscarPorMetodoPago(@Param("metodoPago") String metodoPago);

    // Ventas recientes desde una fecha
    @Query("SELECT v FROM Venta v WHERE v.fecha >= :fechaInicio ORDER BY v.fecha DESC")
    List<Venta> buscarVentasRecientes(@Param("fechaInicio") LocalDateTime fechaInicio);

    // =========================================================
    // CONSULTAS DE ESTADÍSTICAS Y AGREGACIÓN 📊
    // =========================================================

    // Total vendido históricamente por un empleado
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v WHERE v.empleado.id = :empleadoId")
    BigDecimal obtenerTotalVendidoPorEmpleado(@Param("empleadoId") Long empleadoId);

    // Total vendido por un empleado en un rango de fechas
    @Query("""
            SELECT COALESCE(SUM(v.totalVenta), 0)
            FROM Venta v
            WHERE v.empleado.id = :empleadoId
              AND v.fecha >= :inicio 
              AND v.fecha < :fin
            """)
    BigDecimal obtenerTotalVendidoPorEmpleadoEntreFechas(
            @Param("empleadoId") Long empleadoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Total de ventas en un rango de tiempo
    @Query("""
            SELECT COALESCE(SUM(v.totalVenta), 0)
            FROM Venta v
            WHERE v.fecha >= :inicio 
              AND v.fecha < :fin
            """)
    BigDecimal totalVentasEnRango(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Total de ventas históricas
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v")
    BigDecimal totalVentasHistoricas();

    // Total vendido por todos los empleados
    @Query("""
           SELECT v.empleado.nombre, COALESCE(SUM(v.totalVenta), 0)
           FROM Venta v 
           GROUP BY v.empleado.nombre 
           ORDER BY 2 DESC
           """)
    List<Object[]> obtenerTotalVendidoPorTodosLosEmpleados();

    // Cantidad vendida por empleado y producto
    @Query("""
            SELECT v.empleado.nombre,
                   p.nombre,
                   COALESCE(SUM(d.cantidad), 0)
            FROM Venta v
            JOIN v.detalles d
            JOIN d.inventario i
            JOIN i.producto p
            GROUP BY v.empleado.nombre, p.nombre
            ORDER BY 1, 3 DESC
            """)
    List<Object[]> obtenerCantidadVendidaPorEmpleadoYProducto();
}
