package com.example.schedule.repository;

import com.example.schedule.model.BloqueHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Long> {
    List<BloqueHorario> findByEsRecreoFalseOrderByNumeroAsc();
    List<BloqueHorario> findAllByOrderByNumeroAsc();
}
