package com.example.schedule.service;

import com.example.schedule.dto.EstudianteRequest;
import com.example.schedule.dto.EstudianteResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Apoderado;
import com.example.schedule.model.Curso;
import com.example.schedule.model.Estudiante;
import com.example.schedule.repository.ApoderadoRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final ApoderadoRepository apoderadoRepository;

    public Page<EstudianteResponse> findAll(Pageable pageable) {
        return estudianteRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<EstudianteResponse> findByCurso(Long cursoId, Pageable pageable) {
        return estudianteRepository.findByCursoId(cursoId, pageable).map(this::toResponse);
    }

    public EstudianteResponse findById(Long id) {
        return toResponse(getById(id));
    }

    public EstudianteResponse create(EstudianteRequest request) {
        Curso curso = null;
        if (request.getCursoId() != null) {
            curso = cursoRepository.findById(request.getCursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + request.getCursoId()));
        }
        Apoderado apoderado = resolverApoderado(request.getApoderadoId());
        Estudiante estudiante = Estudiante.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .rut(request.getRut())
                .fechaNacimiento(request.getFechaNacimiento())
                .curso(curso)
                .apoderado(apoderado)
                .build();
        return toResponse(estudianteRepository.save(estudiante));
    }

    public EstudianteResponse update(Long id, EstudianteRequest request) {
        Estudiante estudiante = getById(id);
        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setRut(request.getRut());
        estudiante.setFechaNacimiento(request.getFechaNacimiento());
        if (request.getCursoId() != null) {
            Curso curso = cursoRepository.findById(request.getCursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + request.getCursoId()));
            estudiante.setCurso(curso);
        }
        estudiante.setApoderado(resolverApoderado(request.getApoderadoId()));
        return toResponse(estudianteRepository.save(estudiante));
    }

    public void delete(Long id) {
        Estudiante estudiante = getById(id);
        estudianteRepository.delete(estudiante);
    }

    private Estudiante getById(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
    }

    private Apoderado resolverApoderado(Long apoderadoId) {
        if (apoderadoId == null) {
            return null;
        }
        return apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con id: " + apoderadoId));
    }

    private EstudianteResponse toResponse(Estudiante e) {
        return EstudianteResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .rut(e.getRut())
                .fechaNacimiento(e.getFechaNacimiento())
                .cursoId(e.getCurso() != null ? e.getCurso().getId() : null)
                .nombreCurso(e.getCurso() != null ? e.getCurso().getNombre() : null)
                .apoderadoId(e.getApoderado() != null ? e.getApoderado().getId() : null)
                .nombreApoderado(e.getApoderado() != null
                        ? e.getApoderado().getNombre() + " " + e.getApoderado().getApellidoPaterno()
                        : null)
                .build();
    }
}
