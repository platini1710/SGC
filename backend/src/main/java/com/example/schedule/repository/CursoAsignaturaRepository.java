package com.example.schedule.repository;

import com.example.schedule.model.CursoAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoAsignaturaRepository extends JpaRepository<CursoAsignatura, Long> {
    List<CursoAsignatura> findByCursoId(Long cursoId);
    Optional<CursoAsignatura> findByCursoIdAndAsignaturaId(Long cursoId, Long asignaturaId);
    boolean existsByCursoIdAndAsignaturaId(Long cursoId, Long asignaturaId);
    boolean existsByCursoIdAndAsignaturaIdAndProfesorId(Long cursoId, Long asignaturaId, Long profesorId);
    boolean existsByCursoIdAndProfesorId(Long cursoId, Long profesorId);
    List<CursoAsignatura> findByProfesorId(Long profesorId);
}
