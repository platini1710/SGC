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

        boolean conflictoProfesor =
                horarioRepository.existsByProfesorIdAndDiaSemanaAndBloqueIdAndIdNot(nuevoProfesorId, nuevoDia, nuevoBloqueId, horarioId);

        // La constraint única (profesor_id, dia_semana, bloque_id) sólo cubre el profesor
        // primario. Para los medios bloques (subtramo secundario) validamos en memoria si el
        // profesor destino ya ocupa esa ranura como primario o como secundario de otra fila.
        if (!conflictoProfesor) {
            for (Horario fila : horarioRepository.findByDiaSemanaAndBloqueId(nuevoDia, nuevoBloqueId)) {
                if (fila.getId().equals(horarioId)) {
                    continue;
                }
                boolean coincidePrimario = fila.getProfesor() != null
                        && fila.getProfesor().getId().equals(nuevoProfesorId);
                boolean coincideSecundario = fila.getProfesorSecundaria() != null
                        && fila.getProfesorSecundaria().getId().equals(nuevoProfesorId);
                if (coincidePrimario || coincideSecundario) {
                    conflictoProfesor = true;
                    break;
                }
            }
        }

        if (conflictoProfesor) {
            conflictos.add(ConflictoResponse.builder()
                    .tipo("PROFESOR")
                    .descripcion("El profesor ya tiene clase en ese bloque")
                    .build());
        }

        return conflictos;
    }
}
