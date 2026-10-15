package com.example.schedule.repository;

import com.example.schedule.model.Estudiante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Page<Estudiante> findByCursoId(Long cursoId, Pageable pageable);
    List<Estudiante> findByCursoIdOrderByApellidoAscNombreAsc(Long cursoId);
    Optional<Estudiante> findByRut(String rut);
    List<Estudiante> findByApoderadoId(Long apoderadoId);
    int countByApoderadoId(Long apoderadoId);
}
