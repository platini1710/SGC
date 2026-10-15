package com.example.schedule.service;

import com.example.schedule.dto.EspecialidadRequest;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Especialidad;
import com.example.schedule.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public List<Especialidad> findAll() {
        return especialidadRepository.findAll();
    }

    public Especialidad findById(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
    }

    public Especialidad create(EspecialidadRequest request) {
        Especialidad especialidad = Especialidad.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return especialidadRepository.save(especialidad);
    }

    public Especialidad update(Long id, EspecialidadRequest request) {
        Especialidad especialidad = findById(id);
        especialidad.setNombre(request.getNombre());
        especialidad.setDescripcion(request.getDescripcion());
        return especialidadRepository.save(especialidad);
    }

    public void delete(Long id) {
        Especialidad especialidad = findById(id);
        especialidadRepository.delete(especialidad);
    }
}
