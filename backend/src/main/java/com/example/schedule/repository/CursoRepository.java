package com.example.schedule.repository;

import com.example.schedule.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findAllByOrderByNivelAsc();
    Optional<Curso> findByNivel(Integer nivel);
    boolean existsByPlanEstudioId(Long id);
}
