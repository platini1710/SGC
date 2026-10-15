package com.example.schedule.service;

import com.example.schedule.dto.ApoderadoRequest;
import com.example.schedule.dto.ApoderadoResponse;
import com.example.schedule.exception.DuplicadoException;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Apoderado;
import com.example.schedule.model.Estudiante;
import com.example.schedule.repository.ApoderadoRepository;
import com.example.schedule.repository.EstudianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApoderadoService {

    private static final String CORREO_DEFAULT = "aespinoza3010@gmail.com";

    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteRepository estudianteRepository;

    public List<ApoderadoResponse> findAll() {
        return apoderadoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ApoderadoResponse findById(Long id) {
        return toResponse(getById(id));
    }

    public ApoderadoResponse create(ApoderadoRequest request) {
        if (apoderadoRepository.existsByRut(request.getRut())) {
            throw new DuplicadoException("Ya existe un apoderado con el RUT: " + request.getRut());
        }
        Apoderado apoderado = Apoderado.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .region(request.getRegion())
                .comuna(request.getComuna())
                .direccion(request.getDireccion())
                .telefonoCelular(request.getTelefonoCelular())
                .correo(resolverCorreo(request.getCorreo()))
                .build();
        return toResponse(apoderadoRepository.save(apoderado));
    }

    public ApoderadoResponse update(Long id, ApoderadoRequest request) {
        Apoderado apoderado = getById(id);
        if (!apoderado.getRut().equals(request.getRut())
                && apoderadoRepository.existsByRut(request.getRut())) {
            throw new DuplicadoException("Ya existe un apoderado con el RUT: " + request.getRut());
        }
        apoderado.setRut(request.getRut());
        apoderado.setNombre(request.getNombre());
        apoderado.setApellidoPaterno(request.getApellidoPaterno());
        apoderado.setRegion(request.getRegion());
        apoderado.setComuna(request.getComuna());
        apoderado.setDireccion(request.getDireccion());
        apoderado.setTelefonoCelular(request.getTelefonoCelular());
        apoderado.setCorreo(resolverCorreo(request.getCorreo()));
        return toResponse(apoderadoRepository.save(apoderado));
    }

    @Transactional
    public void delete(Long id) {
        Apoderado apoderado = getById(id);
        List<Estudiante> estudiantes = estudianteRepository.findByApoderadoId(id);
        for (Estudiante estudiante : estudiantes) {
            estudiante.setApoderado(null);
        }
        estudianteRepository.saveAll(estudiantes);
        apoderadoRepository.delete(apoderado);
    }

    private Apoderado getById(Long id) {
        return apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con id: " + id));
    }

    private String resolverCorreo(String correo) {
        return (correo == null || correo.isBlank()) ? CORREO_DEFAULT : correo;
    }

    private ApoderadoResponse toResponse(Apoderado a) {
        return ApoderadoResponse.builder()
                .id(a.getId())
                .rut(a.getRut())
                .nombre(a.getNombre())
                .apellidoPaterno(a.getApellidoPaterno())
                .region(a.getRegion())
                .comuna(a.getComuna())
                .direccion(a.getDireccion())
                .telefonoCelular(a.getTelefonoCelular())
                .correo(a.getCorreo())
                .cantidadEstudiantes(estudianteRepository.countByApoderadoId(a.getId()))
                .build();
    }
}
