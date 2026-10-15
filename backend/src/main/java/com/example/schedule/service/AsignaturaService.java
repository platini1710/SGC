package com.example.schedule.service;

import com.example.schedule.dto.AsignaturaRequest;
import com.example.schedule.dto.AsignaturaResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Asignatura;
import com.example.schedule.repository.AsignaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;

    public List<AsignaturaResponse> findAll() {
        return asignaturaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AsignaturaResponse findById(Long id) {
        return toResponse(getById(id));
    }

    public AsignaturaResponse create(AsignaturaRequest request) {
        Asignatura asignatura = Asignatura.builder()
                .nombre(request.getNombre())
                .codigo(request.getCodigo())
                .descripcion(request.getDescripcion())
                .build();
        return toResponse(asignaturaRepository.save(asignatura));
    }

    public AsignaturaResponse update(Long id, AsignaturaRequest request) {
        Asignatura asignatura = getById(id);
        asignatura.setNombre(request.getNombre());
        asignatura.setCodigo(request.getCodigo());
        asignatura.setDescripcion(request.getDescripcion());
        return toResponse(asignaturaRepository.save(asignatura));
    }

    public void delete(Long id) {
        Asignatura asignatura = getById(id);
        asignaturaRepository.delete(asignatura);
    }

    public Asignatura getById(Long id) {
        return asignaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con id: " + id));
    }

    private AsignaturaResponse toResponse(Asignatura a) {
        return AsignaturaResponse.builder()
                .id(a.getId())
                .nombre(a.getNombre())
                .codigo(a.getCodigo())
                .descripcion(a.getDescripcion())
                .build();
    }
}
