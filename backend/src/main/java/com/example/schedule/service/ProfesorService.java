package com.example.schedule.service;

import com.example.schedule.dto.HorarioResponse;
import com.example.schedule.dto.ProfesorRequest;
import com.example.schedule.dto.ProfesorResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Horario;
import com.example.schedule.model.Profesor;
import com.example.schedule.repository.EspecialidadRepository;
import com.example.schedule.repository.HorarioRepository;
import com.example.schedule.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final HorarioRepository horarioRepository;

    public List<ProfesorResponse> findAll() {
        return profesorRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProfesorResponse findById(Long id) {
        return toResponse(getById(id));
    }

    public ProfesorResponse create(ProfesorRequest request) {
        Profesor profesor = Profesor.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .rut(request.getRut())
                .activo(true)
                .especialidades(especialidadRepository.findAllById(
                        request.getEspecialidadIds() != null ? request.getEspecialidadIds() : List.of()))
                .build();
        return toResponse(profesorRepository.save(profesor));
    }

    public ProfesorResponse update(Long id, ProfesorRequest request) {
        Profesor profesor = getById(id);
        profesor.setNombre(request.getNombre());
        profesor.setApellido(request.getApellido());
        profesor.setEmail(request.getEmail());
        profesor.setRut(request.getRut());
        profesor.setEspecialidades(especialidadRepository.findAllById(
                request.getEspecialidadIds() != null ? request.getEspecialidadIds() : List.of()));
        return toResponse(profesorRepository.save(profesor));
    }

    public void delete(Long id) {
        Profesor profesor = getById(id);
        profesor.setActivo(false);
        profesorRepository.save(profesor);
    }

    public List<HorarioResponse> getHorario(Long profesorId) {
        getById(profesorId); // validate existence
        return horarioRepository.findByProfesorId(profesorId).stream()
                .map(this::toHorarioResponse)
                .toList();
    }

    public Profesor getById(Long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con id: " + id));
    }

    private ProfesorResponse toResponse(Profesor p) {
        return ProfesorResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .apellido(p.getApellido())
                .email(p.getEmail())
                .rut(p.getRut())
                .activo(p.getActivo())
                .especialidades(p.getEspecialidades().stream()
                        .map(e -> e.getNombre())
                        .toList())
                .build();
    }

    private HorarioResponse toHorarioResponse(Horario h) {
        return HorarioResponse.builder()
                .id(h.getId())
                .cursoId(h.getCurso().getId())
                .nombreCurso(h.getCurso().getNombre())
                .asignaturaId(h.getAsignatura().getId())
                .nombreAsignatura(h.getAsignatura().getNombre())
                .profesorId(h.getProfesor().getId())
                .nombreProfesor(h.getProfesor().getNombre() + " " + h.getProfesor().getApellido())
                .diaSemana(h.getDiaSemana())
                .bloqueId(h.getBloque().getId())
                .bloqueNumero(h.getBloque().getNumero())
                .jornada(h.getBloque().getJornada())
                .horaInicio(h.getBloque().getHoraInicio())
                .horaFin(h.getBloque().getHoraFin())
                .editadoManualmente(h.getEditadoManualmente())
                .esMedioBloque(Boolean.TRUE.equals(h.getEsMedioBloque()))
                .asignaturaSecId(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getId() : null)
                .nombreAsignaturaSec(h.getAsignaturaSecundaria() != null ? h.getAsignaturaSecundaria().getNombre() : null)
                .profesorSecId(h.getProfesorSecundaria() != null ? h.getProfesorSecundaria().getId() : null)
                .nombreProfesorSec(h.getProfesorSecundaria() != null
                        ? h.getProfesorSecundaria().getNombre() + " " + h.getProfesorSecundaria().getApellido() : null)
                .build();
    }
}
