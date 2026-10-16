package com.example.schedule.repository;

import com.example.schedule.model.NotaFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotaFinalRepository extends JpaRepository<NotaFinal, Long> {
    Optional<NotaFinal> findByEstudianteIdAndAsignaturaIdAndAnio(
            Long estudianteId, Long asignaturaId, int anio);

    List<NotaFinal> findByAsignaturaIdAndAnio(Long asignaturaId, int anio);
}
