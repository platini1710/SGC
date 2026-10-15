package com.example.schedule.repository;

import com.example.schedule.model.Trimestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrimestreRepository extends JpaRepository<Trimestre, Long> {
    List<Trimestre> findByAnioOrderByNumeroAsc(int anio);
    Optional<Trimestre> findByNumeroAndAnio(int numero, int anio);
}
