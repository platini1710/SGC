package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash", "faceDescriptor"})
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    // Descriptor facial (128 valores) serializado como arreglo JSON. Null si no hay rostro registrado.
    @Column(name = "face_descriptor", columnDefinition = "text")
    private String faceDescriptor;

    // Intentos de login fallidos consecutivos (contraseña y rostro comparten el contador).
    @Column(name = "intentos_fallidos", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int intentosFallidos = 0;

    // Bloqueo permanente: solo un ADMIN puede quitarlo.
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean bloqueado = false;

    // Bloqueo temporal (usuarios ADMIN): vence solo en esta fecha/hora.
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // RF-01.2: fecha del último cambio de contraseña (para la vigencia de 6 meses).
    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    // RF-01: fuerza el cambio de contraseña en el próximo login (expiración o alta inicial).
    @Column(name = "must_change_password", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean mustChangePassword = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Spring Security revisa esto ANTES de validar la contraseña: una cuenta bloqueada
    // no entra ni con la contraseña correcta.
    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado && !bloqueoTemporalVigente();
    }

    public boolean bloqueoTemporalVigente() {
        return bloqueadoHasta != null && LocalDateTime.now().isBefore(bloqueadoHasta);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
