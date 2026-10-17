package com.example.schedule.config;

import com.example.schedule.model.Asignatura;
import com.example.schedule.model.BloqueHorario;
import com.example.schedule.model.Curso;
import com.example.schedule.model.CursoAsignatura;
import com.example.schedule.model.PlanEstudio;
import com.example.schedule.repository.BloqueHorarioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.PlanEstudioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NIT-1: el perfil de test usa H2 con ddl-auto=create-drop, de modo que las columnas nuevas
// (jornada, campos JEC de Parametro) nacen siempre frescas y NO se ejercitan las ramas de
// backfill de NULL (ensureJornadaRegular / backfill incondicional de Parametro). Esas ramas
// sólo importan en PostgreSQL con ddl-auto=update y se validan por razonamiento / producción.
@SpringBootTest
@ActiveProfiles("test")
class DataInitializerSeedITTest {

    @Autowired
    private PlanEstudioRepository planEstudioRepository;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Autowired
    private BloqueHorarioRepository bloqueHorarioRepository;

    @Test
    void seedCreaTresPlanesConSusAsignaturas() {
        assertEquals(3, planEstudioRepository.count(), "Deben existir 3 planes de estudio");

        PlanEstudio plan1 = porNombre("Plan de Estudio 1° a 4° Básico");
        PlanEstudio plan2 = porNombre("Plan de Estudio 5° a 6° Básico");
        PlanEstudio plan3 = porNombre("Plan de Estudio 7° a 8° Básico");

        List<Asignatura> plan1Asigs = asignaturasDe(plan1);
        assertEquals(11, plan1Asigs.size(), "Plan 1°-4° debe tener 11 asignaturas (con Religión y Taller JEC)");
        assertEquals(10, asignaturasDe(plan2).size(), "Plan 5°-6° debe tener 10 asignaturas");
        assertEquals(10, asignaturasDe(plan3).size(), "Plan 7°-8° debe tener 10 asignaturas");

        assertTrue(plan1Asigs.stream().anyMatch(a -> "TAL".equals(a.getCodigo())),
                "Taller JEC (TAL) debe pertenecer al plan 1°-4°");
        assertTrue(plan1Asigs.stream().anyMatch(a -> "REL".equals(a.getCodigo())),
                "Religión (REL) debe pertenecer al plan 1°-4°");
        assertTrue(plan1Asigs.stream().anyMatch(a -> "Taller JEC (Libre Disposición)".equals(a.getNombre())),
                "La asignatura 'Taller JEC (Libre Disposición)' debe existir en el plan 1°-4°");

        assertEquals("628", plan3.getDecreto());
    }

    @Test
    void seedCreaBloquesJecConSusTiempos() {
        assertEquals(7, bloqueHorarioRepository.countByJornada("JEC"),
                "Deben existir 7 bloques JEC (4 clase + 3 pausas)");
        assertEquals(8, bloqueHorarioRepository.countByJornada("REGULAR"),
                "Deben existir 8 bloques REGULAR");

        List<BloqueHorario> clasesJec =
                bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC");
        assertEquals(4, clasesJec.size(), "Deben existir 4 bloques de clase JEC");
        assertEquals(LocalTime.of(8, 0), clasesJec.get(0).getHoraInicio());
        assertEquals(LocalTime.of(9, 30), clasesJec.get(0).getHoraFin());
        assertEquals(LocalTime.of(9, 45), clasesJec.get(1).getHoraInicio());
        assertEquals(LocalTime.of(11, 15), clasesJec.get(1).getHoraFin());
        assertEquals(LocalTime.of(11, 30), clasesJec.get(2).getHoraInicio());
        assertEquals(LocalTime.of(13, 0), clasesJec.get(2).getHoraFin());
        assertEquals(LocalTime.of(14, 15), clasesJec.get(3).getHoraInicio());
        assertEquals(LocalTime.of(15, 45), clasesJec.get(3).getHoraFin());
    }

    @Test
    @Transactional
    void cursos1a4TienenLasCargasJec() {
        for (Curso curso : cursoRepository.findAllByOrderByNivelAsc()) {
            if (curso.getNivel() > 4) continue;
            List<CursoAsignatura> cargas = cursoAsignaturaRepository.findByCursoId(curso.getId());
            assertEquals(11, cargas.size(),
                    "El curso " + curso.getNombre() + " debe tener 11 asignaturas JEC");
            int totalHoras = cargas.stream().mapToInt(CursoAsignatura::getHorasSemanales).sum();
            assertEquals(38, totalHoras,
                    "El curso " + curso.getNombre() + " debe sumar 38 horas pedagógicas");
            assertEquals(8, horasDe(cargas, "LEN"), "Lenguaje = 8 horas");
            assertEquals(4, horasDe(cargas, "EF"), "Ed. Física = 4 horas");
            assertEquals(2, horasDe(cargas, "REL"), "Religión = 2 horas");
            assertEquals(6, horasDe(cargas, "TAL"), "Taller JEC = 6 horas");
        }
    }

    private int horasDe(List<CursoAsignatura> cargas, String codigo) {
        return cargas.stream()
                .filter(ca -> codigo.equals(ca.getAsignatura().getCodigo()))
                .mapToInt(CursoAsignatura::getHorasSemanales)
                .findFirst()
                .orElse(-1);
    }

    @Test
    @Transactional
    void cadaCursoTienePlanCorrectoPorNivel() {
        for (Curso curso : cursoRepository.findAllByOrderByNivelAsc()) {
            int nivel = curso.getNivel();
            assertNotNull(curso.getPlanEstudio(), "El curso nivel " + nivel + " debe tener plan");
            int desde = curso.getPlanEstudio().getNivelDesde();
            int hasta = curso.getPlanEstudio().getNivelHasta();
            assertTrue(nivel >= desde && nivel <= hasta,
                    "El nivel " + nivel + " debe caer dentro del rango de su plan [" + desde + "," + hasta + "]");
        }
    }

    @Test
    @Transactional
    void todaCargaSembradaEstaDentroDelPlanDelCurso() {
        List<CursoAsignatura> cargas = cursoAsignaturaRepository.findAll();
        assertTrue(!cargas.isEmpty(), "Debe haber cargas sembradas");
        for (CursoAsignatura ca : cargas) {
            Set<Long> idsPlan = planEstudioRepository
                    .findByIdFetchAsignaturas(ca.getCurso().getPlanEstudio().getId())
                    .map(p -> p.getAsignaturas().stream().map(Asignatura::getId).collect(Collectors.toSet()))
                    .orElseThrow();
            assertTrue(idsPlan.contains(ca.getAsignatura().getId()),
                    "La asignatura '" + ca.getAsignatura().getNombre() + "' del curso '"
                            + ca.getCurso().getNombre() + "' no pertenece a su plan de estudio");
        }
    }

    private PlanEstudio porNombre(String nombre) {
        Optional<PlanEstudio> p = planEstudioRepository.findByNombre(nombre);
        assertTrue(p.isPresent(), "Debe existir el plan: " + nombre);
        return p.get();
    }

    private List<Asignatura> asignaturasDe(PlanEstudio plan) {
        return planEstudioRepository.findByIdFetchAsignaturas(plan.getId()).orElseThrow().getAsignaturas();
    }
}
