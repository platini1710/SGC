package com.example.schedule.repository;

import com.example.schedule.model.PlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {
    Optional<PlanEstudio> findByNombre(String nombre);

    @Query("select p from PlanEstudio p left join fetch p.asignaturas where p.id = :id")
    Optional<PlanEstudio> findByIdFetchAsignaturas(@Param("id") Long id);

    List<PlanEstudio> findAllByOrderByNivelDesdeAsc();
}
