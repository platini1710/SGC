package com.example.schedule.config;

import com.example.schedule.model.Asignatura;
import com.example.schedule.model.Curso;
import com.example.schedule.model.CursoAsignatura;
import com.example.schedule.model.PlanEstudio;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.PlanEstudioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DataInitializerSeedITTest {

    @Autowired
    private PlanEstudioRepository planEstudioRepository;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private CursoAsignaturaRepository cursoAsignaturaRepository;

    @Test
    void seedCreaTresPlanesConSusAsignaturas() {
        assertEquals(3, planEstudioRepository.count(), "Deben existir 3 planes de estudio");

        PlanEstudio plan1 = porNombre("Plan de Estudio 1° a 4° Básico");
        PlanEstudio plan2 = porNombre("Plan de Estudio 5° a 6° Básico");
        PlanEstudio plan3 = porNombre("Plan de Estudio 7° a 8° Básico");

        assertEquals(9, asignaturasDe(plan1).size(), "Plan 1°-4° debe tener 9 asignaturas");
        assertEquals(10, asignaturasDe(plan2).size(), "Plan 5°-6° debe tener 10 asignaturas");
        assertEquals(10, asignaturasDe(plan3).size(), "Plan 7°-8° debe tener 10 asignaturas");

        assertEquals("628", plan3.getDecreto());
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
