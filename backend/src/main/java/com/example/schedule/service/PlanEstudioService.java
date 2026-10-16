package com.example.schedule.service;

import com.example.schedule.dto.AsignaturaResponse;
import com.example.schedule.dto.PlanEstudioDetalleResponse;
import com.example.schedule.dto.PlanEstudioRequest;
import com.example.schedule.dto.PlanEstudioResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Asignatura;
import com.example.schedule.model.PlanEstudio;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.PlanEstudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanEstudioService {

    private final PlanEstudioRepository planEstudioRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final CursoRepository cursoRepository;

    @Transactional(readOnly = true)
    public List<PlanEstudioResponse> findAll() {
        return planEstudioRepository.findAllByOrderByNivelDesdeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanEstudioDetalleResponse findById(Long id) {
        PlanEstudio plan = getByIdFetch(id);
        return toDetalle(plan);
    }

    @Transactional
    public PlanEstudioResponse create(PlanEstudioRequest req) {
        if (req.getNivelDesde() > req.getNivelHasta()) {
            throw new SchedulingException("El nivel inicial no puede ser mayor al nivel final");
        }
        PlanEstudio plan = PlanEstudio.builder()
                .nombre(req.getNombre())
                .descripcion(req.getDescripcion())
                .decreto(req.getDecreto())
                .nivelDesde(req.getNivelDesde())
                .nivelHasta(req.getNivelHasta())
                .activo(req.getActivo() != null ? req.getActivo() : Boolean.TRUE)
                .build();
        return toResponse(planEstudioRepository.save(plan));
    }

    @Transactional
    public PlanEstudioResponse update(Long id, PlanEstudioRequest req) {
        if (req.getNivelDesde() > req.getNivelHasta()) {
            throw new SchedulingException("El nivel inicial no puede ser mayor al nivel final");
        }
        PlanEstudio plan = getById(id);
        plan.setNombre(req.getNombre());
        plan.setDescripcion(req.getDescripcion());
        plan.setDecreto(req.getDecreto());
        plan.setNivelDesde(req.getNivelDesde());
        plan.setNivelHasta(req.getNivelHasta());
        if (req.getActivo() != null) {
            plan.setActivo(req.getActivo());
        }
        return toResponse(planEstudioRepository.save(plan));
    }

    @Transactional
    public PlanEstudioDetalleResponse addAsignatura(Long planId, Long asignaturaId) {
        PlanEstudio plan = getByIdFetch(planId);
        Asignatura asignatura = asignaturaRepository.findById(asignaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con id: " + asignaturaId));
        boolean yaEsta = plan.getAsignaturas().stream()
                .anyMatch(a -> a.getId().equals(asignaturaId));
        if (!yaEsta) {
            plan.getAsignaturas().add(asignatura);
            planEstudioRepository.save(plan);
        }
        return toDetalle(plan);
    }

    @Transactional
    public PlanEstudioDetalleResponse removeAsignatura(Long planId, Long asignaturaId) {
        PlanEstudio plan = getByIdFetch(planId);
        boolean removido = plan.getAsignaturas().removeIf(a -> a.getId().equals(asignaturaId));
        if (!removido) {
            throw new ResourceNotFoundException(
                    "La asignatura " + asignaturaId + " no pertenece al plan " + planId);
        }
        planEstudioRepository.save(plan);
        return toDetalle(plan);
    }

    @Transactional
    public void delete(Long id) {
        PlanEstudio plan = getById(id);
        if (cursoRepository.existsByPlanEstudioId(id)) {
            throw new SchedulingException(
                    "No se puede eliminar el plan: está asignado a uno o más cursos. Reasigne esos cursos primero.");
        }
        planEstudioRepository.delete(plan);
    }

    private PlanEstudio getById(Long id) {
        return planEstudioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado con id: " + id));
    }

    private PlanEstudio getByIdFetch(Long id) {
        return planEstudioRepository.findByIdFetchAsignaturas(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado con id: " + id));
    }

    private PlanEstudioResponse toResponse(PlanEstudio p) {
        return PlanEstudioResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .decreto(p.getDecreto())
                .nivelDesde(p.getNivelDesde())
                .nivelHasta(p.getNivelHasta())
                .activo(p.getActivo())
                .cantidadAsignaturas(p.getAsignaturas().size())
                .build();
    }

    private PlanEstudioDetalleResponse toDetalle(PlanEstudio p) {
        List<AsignaturaResponse> asignaturas = p.getAsignaturas().stream()
                .map(a -> AsignaturaResponse.builder()
                        .id(a.getId())
                        .nombre(a.getNombre())
                        .codigo(a.getCodigo())
                        .descripcion(a.getDescripcion())
                        .build())
                .toList();
        return PlanEstudioDetalleResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .decreto(p.getDecreto())
                .nivelDesde(p.getNivelDesde())
                .nivelHasta(p.getNivelHasta())
                .activo(p.getActivo())
                .cantidadAsignaturas(asignaturas.size())
                .asignaturas(asignaturas)
                .build();
    }
}
