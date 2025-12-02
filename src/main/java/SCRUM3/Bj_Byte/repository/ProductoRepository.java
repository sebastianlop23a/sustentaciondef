package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // 🔹 Método existente para búsqueda por nombre
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    // 🚨 NUEVO MÉTODO CRÍTICO PARA EL FILTRO DE VENTAS 🚨
    /**
     * Obtiene una lista de todos los nombres de productos distintos y ordenados.
     * Se usa para poblar el dropdown de filtro en listar_ventas.html.
     */
    @Query("SELECT DISTINCT p.nombre FROM Producto p ORDER BY p.nombre ASC")
    List<String> findDistinctNames();
}