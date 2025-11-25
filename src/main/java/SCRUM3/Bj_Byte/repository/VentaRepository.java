package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // 🔹 Ventas realizadas por un empleado específico
    List<Venta> findByEmpleadoId(Long empleadoId);

    // 🔹 Ventas realizadas entre dos fechas
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    // 🔹 Filtrar ventas por producto (buscando por nombre del producto, que está en Producto)
    @Query("""
           SELECT v
           FROM Venta v
           JOIN v.detalles d
           JOIN d.inventario i
           JOIN i.producto p
           WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombreProducto, '%'))
           """)
    List<Venta> buscarPorProducto(@Param("nombreProducto") String nombreProducto);

    // 🔹 Filtrar ventas por cliente
    @Query("SELECT v FROM Venta v WHERE LOWER(v.cliente) LIKE LOWER(CONCAT('%', :cliente, '%'))")
    List<Venta> buscarPorCliente(@Param("cliente") String cliente);

    // 🔹 Total vendido históricamente por un empleado
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v WHERE v.empleado.id = :empleadoId")
    BigDecimal obtenerTotalVendidoPorEmpleado(@Param("empleadoId") Long empleadoId);

    // 🔹 Total vendido por un empleado en un rango de fechas específico
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) " +
           "FROM Venta v " +
           "WHERE v.empleado.id = :empleadoId " +
           "AND v.fecha >= :inicio AND v.fecha < :fin")
    BigDecimal obtenerTotalVendidoPorEmpleadoEntreFechas(@Param("empleadoId") Long empleadoId,
                                                         @Param("inicio") LocalDateTime inicio,
                                                         @Param("fin") LocalDateTime fin);

    // 🔹 Total de ventas en un rango de tiempo (para estadísticas generales)
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v " +
           "WHERE v.fecha >= :inicio AND v.fecha < :fin")
    BigDecimal totalVentasEnRango(@Param("inicio") LocalDateTime inicio,
                                  @Param("fin") LocalDateTime fin);

    // 🔹 Total de ventas históricas (todas las ventas)
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v")
    BigDecimal totalVentasHistoricas();

    // 🔹 Total vendido por cada empleado (para dashboards o reportes)
    @Query("SELECT v.empleado.nombre, COALESCE(SUM(v.totalVenta), 0) " +
           "FROM Venta v GROUP BY v.empleado.nombre")
    List<Object[]> obtenerTotalVendidoPorTodosLosEmpleados();

    // 🔹 Cantidad vendida por empleado y por producto
    //    Recorre Venta -> detalles -> inventario -> producto
    @Query("""
           SELECT v.empleado.nombre,
                  p.nombre,
                  COALESCE(SUM(d.cantidad), 0)
           FROM Venta v
           JOIN v.detalles d
           JOIN d.inventario i
           JOIN i.producto p
           GROUP BY v.empleado.nombre, p.nombre
           """)
    List<Object[]> obtenerCantidadVendidaPorEmpleadoYProducto();

    // 🔹 Ventas filtradas por método de pago
    @Query("SELECT v FROM Venta v WHERE v.metodoPago = :metodoPago")
    List<Venta> buscarPorMetodoPago(@Param("metodoPago") String metodoPago);

    // 🔹 Buscar ventas recientes (últimos X días)
    @Query("SELECT v FROM Venta v WHERE v.fecha >= :fechaInicio ORDER BY v.fecha DESC")
    List<Venta> buscarVentasRecientes(@Param("fechaInicio") LocalDateTime fechaInicio);
}
