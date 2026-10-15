package com.example.schedule.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "colegio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Colegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String comuna;

    @Column(length = 200)
    private String direccion;

    // Logo como data URI Base64 (ej. "data:image/png;base64,....").
    // TEXT para permitir imágenes grandes. NO usar @Lob: en PostgreSQL
    // @Lob sobre String crea un Large Object (oid) que no puede leerse en
    // modo auto-commit (fuera de transacción), lo que rompía GET /api/colegio
    // con "Large Objects may not be used in auto-commit mode". Mapeado como
    // columna TEXT normal se lee inline sin necesidad de transacción.
    @Column(columnDefinition = "TEXT")
    private String logo;
}
