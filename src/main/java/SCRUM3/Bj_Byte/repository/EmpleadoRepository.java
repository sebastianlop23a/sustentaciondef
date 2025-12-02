package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    // Buscar por correo
    Optional<Empleado> findByCorreo(String correo);

    // Buscar solo empleados activos
    List<Empleado> findByActivoTrue();

    // Buscar por correo y solo si está activo (retorna lista)
    List<Empleado> findByCorreoAndActivoTrueOrderByIdDesc(String correo);

    // Buscar por correo y solo si está activo (retorna Optional)
    Optional<Empleado> findByCorreoAndActivoTrue(String correo);
}
