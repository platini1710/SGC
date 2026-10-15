package com.example.schedule.service;

import com.example.schedule.dto.ConflictoResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Horario;
import com.example.schedule.repository.HorarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConflictValidatorService {

    private final HorarioRepository horarioRepository;

    public List<ConflictoResponse> validarMovimiento(Long horarioId, int nuevoDia, Long nuevoBloqueId, Long nuevoProfesorId) {
        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado con id: " + horarioId));

        List<ConflictoResponse> conflictos = new ArrayList<>();

        Long cursoId = horario.getCurso().getId();
        if (horarioRepository.existsByCursoIdAndDiaSemanaAndBloqueIdAndIdNot(cursoId, nuevoDia, nuevoBloqueId, horarioId)) {
            conflictos.add(ConflictoResponse.builder()
                    .tipo("CURSO")
                    .descripcion("El curso ya tiene clase en ese bloque")
                    .build());
        }

        if (horarioRepository.existsByProfesorIdAndDiaSemanaAndBloqueIdAndIdNot(nuevoProfesorId, nuevoDia, nuevoBloqueId, horarioId)) {
            conflictos.add(ConflictoResponse.builder()
                    .tipo("PROFESOR")
                    .descripcion("El profesor ya tiene clase en ese bloque")
                    .build());
        }

        return conflictos;
    }
}
