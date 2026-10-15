package com.example.schedule.repository;

import com.example.schedule.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByEstudianteIdAndAsignaturaIdAndTrimestreId(Long estudianteId, Long asignaturaId, Long trimestreId);
    List<Nota> findByAsignaturaIdAndTrimestreId(Long asignaturaId, Long trimestreId);
    long countByEstudianteIdAndAsignaturaIdAndTrimestreId(Long estudianteId, Long asignaturaId, Long trimestreId);
    Optional<Nota> findByEstudianteIdAndAsignaturaIdAndTrimestreIdAndPosicion(
            Long estudianteId, Long asignaturaId, Long trimestreId, Integer posicion);
}
