package com.example.schedule.repository;

import com.example.schedule.model.EstadoMatricula;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    // Upsert / resolución puntual.
    Optional<Matricula> findByEstudianteIdAndAnio(Long estudianteId, int anio);
    boolean existsByEstudianteIdAndAnio(Long estudianteId, int anio);

    // Batch para evitar N+1 al resolver cursos de muchos alumnos (listados/inactivos).
    List<Matricula> findByEstudianteIdInAndAnio(List<Long> estudianteIds, int anio);

    // Fuente de la promoción: todas las matrículas de un año en un estado.
    List<Matricula> findByAnioAndEstado(int anio, EstadoMatricula estado);

    // Listado paginado de estudiantes por (curso, año): filtra ACTIVA + estudiante.activo
    // y FIJA el orden por apellidoPaterno, nombre. El Sort del Pageable entrante se IGNORA
    // (el JPQL es el orden canónico); el Pageable solo aporta page/size.
    @Query(value = "select e from Matricula m join m.estudiante e "
            + "where m.curso.id = :cursoId and m.anio = :anio "
            + "and m.estado = com.example.schedule.model.EstadoMatricula.ACTIVA "
            + "and e.activo = true "
            + "order by e.apellidoPaterno asc, e.nombre asc",
           countQuery = "select count(e) from Matricula m join m.estudiante e "
            + "where m.curso.id = :cursoId and m.anio = :anio "
            + "and m.estado = com.example.schedule.model.EstadoMatricula.ACTIVA "
            + "and e.activo = true")
    Page<Estudiante> findEstudiantesByCursoAndAnio(@Param("cursoId") Long cursoId,
                                                   @Param("anio") int anio,
                                                   Pageable pageable);

    // Variante lista (sin paginar) para grilla/consolidado/reportes: mismo filtro y orden.
    @Query("select e from Matricula m join m.estudiante e "
            + "where m.curso.id = :cursoId and m.anio = :anio "
            + "and m.estado = com.example.schedule.model.EstadoMatricula.ACTIVA "
            + "and e.activo = true "
            + "order by e.apellidoPaterno asc, e.nombre asc")
    List<Estudiante> findEstudiantesByCursoAndAnio(@Param("cursoId") Long cursoId,
                                                   @Param("anio") int anio);
}
