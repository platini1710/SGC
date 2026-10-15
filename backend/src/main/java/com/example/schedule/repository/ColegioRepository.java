package com.example.schedule.repository;

import com.example.schedule.model.Colegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColegioRepository extends JpaRepository<Colegio, Long> {
    // Solo hay una fila de colegio; se toma la primera.
    Optional<Colegio> findTopByOrderByIdAsc();
}
