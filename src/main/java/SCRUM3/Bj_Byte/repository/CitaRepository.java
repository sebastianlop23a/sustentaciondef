package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Contar cuántas citas hay por estado (ejemplo: "Pendiente", "Atendida", "Cancelada")
     */
    long countByEstado(String estado);

    /**
     * Buscar todas las citas con un estado determinado.
     * útil si quieres recorrer los objetos Cita completos (fecha, hora, correo, etc.).
     */
    List<Cita> findByEstado(String estado);

    /**
     * Buscar todas las citas con estado dado y que tengan correo no nulo.
     * útil para evitar intentar enviar correos a registros sin email.
     */
    List<Cita> findByEstadoAndCorreoIsNotNull(String estado);

    /**
     * Obtener la lista de correos (sin duplicados) para un estado dado.
     * ideal para envío masivo (si quieres enviar 1 correo por destinatario).
     */
    @Query("SELECT DISTINCT c.correo FROM Cita c WHERE c.estado = :estado AND c.correo IS NOT NULL")
    List<String> findDistinctCorreosByEstado(@Param("estado") String estado);

    /**
     * Últimas N citas (ordenadas por id descendente).
     */
    List<Cita> findTop10ByOrderByIdDesc();

    /**
     * Todas las citas ordenadas por fecha descendente (siempre que el campo fecha permita ordenación).
     * (Si tu campo fecha es String, revisa el formato para que ordene correctamente).
     */
    List<Cita> findAllByOrderByFechaDesc();

    /**
     * Modificar el estado de una cita (UPDATE).
     * Devuelve el número de filas actualizadas (1 si se encontró el id).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Cita c SET c.estado = :estado WHERE c.id = :id")
    int actualizarEstado(@Param("id") Long id, @Param("estado") String estado);

    List<Cita> findByFechaAndHoraAndCorreo(String fecha, String hora, String correo);

}
