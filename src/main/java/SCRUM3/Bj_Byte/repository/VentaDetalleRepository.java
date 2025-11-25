package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
	java.util.List<VentaDetalle> findByVentaId(Long ventaId);
}
