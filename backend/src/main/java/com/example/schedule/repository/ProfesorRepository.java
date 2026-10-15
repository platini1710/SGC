package com.example.schedule.repository;

import com.example.schedule.model.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    List<Profesor> findByActivoTrue();
    Optional<Profesor> findByEmail(String email);
    Optional<Profesor> findByRut(String rut);
}
