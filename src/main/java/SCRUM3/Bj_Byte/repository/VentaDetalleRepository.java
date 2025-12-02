package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

    /**
     * Encuentra todos los detalles de venta asociados a una Venta específica por su ID.
     */
    java.util.List<VentaDetalle> findByVentaId(Long ventaId);

    // -------------------------------------------------------------------
    // MÉTODO AÑADIDO PARA EL FILTRO DE GANANCIAS POR RANGO DE FECHAS
    // -------------------------------------------------------------------
    /**
     * Obtiene los detalles de ventas (VentaDetalle) cuyas Ventas asociadas
     * cayeron dentro del rango de fechas especificado.
     * * @param start Fecha y hora de inicio del rango (inclusivo).
     * @param end Fecha y hora de fin del rango (exclusivo, usamos < :end).
     * @return Lista de VentaDetalle.
     */
    @Query("SELECT vd FROM VentaDetalle vd JOIN vd.venta v WHERE v.fecha >= :start AND v.fecha < :end")
    List<VentaDetalle> findDetallesInDateRange(
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );
    
}