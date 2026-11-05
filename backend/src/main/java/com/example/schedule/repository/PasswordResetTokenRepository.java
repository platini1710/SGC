package com.example.schedule.repository;

import com.example.schedule.model.PasswordResetToken;
import com.example.schedule.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Último token no usado del usuario (para validar el código ingresado).
    Optional<PasswordResetToken> findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(Usuario usuario);

    List<PasswordResetToken> findByUsuarioAndUsedFalse(Usuario usuario);

    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.usuario = :usuario and t.used = false")
    void invalidarVigentes(Usuario usuario);
}
