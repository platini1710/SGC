package com.example.schedule.repository;

import com.example.schedule.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    List<Usuario> findByPasswordUpdatedAtIsNull();

    // Docente cuyo profesor tiene este correo (para el flujo de cambio de contraseña).
    @org.springframework.data.jpa.repository.Query(
            "select u from Usuario u where u.rol = 'DOCENTE' and lower(u.profesor.email) = lower(:email)")
    Optional<Usuario> findDocenteByProfesorEmailIgnoreCase(String email);

    // Bloqueados de forma permanente o con bloqueo temporal aún vigente.
    List<Usuario> findByBloqueadoTrueOrBloqueadoHastaAfterOrderByUsernameAsc(LocalDateTime ahora);
    boolean existsByUsername(String username);
}
