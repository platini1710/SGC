package com.example.schedule.repository;

import com.example.schedule.model.Horario;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByCursoId(Long cursoId);

    List<Horario> findByProfesorId(Long profesorId);

    boolean existsByCursoIdAndDiaSemanaAndBloqueId(Long cursoId, int diaSemana, Long bloqueId);

    boolean existsByProfesorIdAndDiaSemanaAndBloqueId(Long profesorId, int diaSemana, Long bloqueId);

    boolean existsByCursoIdAndDiaSemanaAndBloqueIdAndIdNot(Long cursoId, int diaSemana, Long bloqueId, Long id);

    boolean existsByProfesorIdAndDiaSemanaAndBloqueIdAndIdNot(Long profesorId, int diaSemana, Long bloqueId, Long id);

    List<Horario> findByDiaSemanaAndBloqueId(int diaSemana, Long bloqueId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Horario h WHERE h.editadoManualmente = false")
    void deleteByEditadoManualmenteFalse();
}
