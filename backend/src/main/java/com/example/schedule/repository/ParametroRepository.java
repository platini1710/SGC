package com.example.schedule.repository;

import com.example.schedule.model.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametroRepository extends JpaRepository<Parametro, Long> {
    // Solo hay una fila de parámetros; se toma la primera.
    Optional<Parametro> findTopByOrderByIdAsc();
}
