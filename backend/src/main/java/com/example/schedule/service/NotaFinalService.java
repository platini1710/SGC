package com.example.schedule.service;

import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.NotaFinal;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.NotaFinalRepository;
import com.example.schedule.repository.TrimestreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mantiene la tabla persistente 'nota_final' con las cuatro notas finales por
 * estudiante+asignatura+año (NF T1, NF T2, NF T3 y NF anual). Los valores se
 * recalculan reutilizando {@link CalculoNotasService} y se guardan (UPSERT) cada
 * vez que se graba una nota.
 */
@Service
@RequiredArgsConstructor
public class NotaFinalService {

    private final NotaFinalRepository notaFinalRepository;
    private final TrimestreRepository trimestreRepository;
    private final EstudianteRepository estudianteRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final CalculoNotasService calculoNotasService;

    /**
     * Recalcula las cuatro notas finales del estudiante+asignatura para el año
     * indicado y las persiste. Si ya existe la fila la actualiza; si no, la crea.
     * Idempotente y respeta la unicidad (estudiante, asignatura, anio).
     */
    public NotaFinal recalcularYGuardar(Long estudianteId, Long asignaturaId, int anio) {
        BigDecimal nfT1 = promedio(estudianteId, asignaturaId, trimestreId(1, anio));
        BigDecimal nfT2 = promedio(estudianteId, asignaturaId, trimestreId(2, anio));
        BigDecimal nfT3 = promedio(estudianteId, asignaturaId, trimestreId(3, anio));
        BigDecimal nfAnual = calculoNotasService.notaFinalAnual(estudianteId, asignaturaId, anio);

        NotaFinal notaFinal = notaFinalRepository
                .findByEstudianteIdAndAsignaturaIdAndAnio(estudianteId, asignaturaId, anio)
                .orElse(null);

        if (notaFinal == null) {
            notaFinal = NotaFinal.builder()
                    .estudiante(estudianteRepository.findById(estudianteId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Estudiante no encontrado con id: " + estudianteId)))
                    .asignatura(asignaturaRepository.findById(asignaturaId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Asignatura no encontrada con id: " + asignaturaId)))
                    .anio(anio)
                    .build();
        }

        notaFinal.setNfT1(nfT1);
        notaFinal.setNfT2(nfT2);
        notaFinal.setNfT3(nfT3);
        notaFinal.setNfAnual(nfAnual);
        notaFinal.setActualizadoEn(LocalDateTime.now());

        return notaFinalRepository.save(notaFinal);
    }

    private Long trimestreId(int numero, int anio) {
        return trimestreRepository.findByNumeroAndAnio(numero, anio)
                .map(Trimestre::getId)
                .orElse(null);
    }

    private BigDecimal promedio(Long estudianteId, Long asignaturaId, Long trimestreId) {
        if (trimestreId == null) {
            return null;
        }
        return calculoNotasService.promedioTrimestre(estudianteId, asignaturaId, trimestreId);
    }
}
