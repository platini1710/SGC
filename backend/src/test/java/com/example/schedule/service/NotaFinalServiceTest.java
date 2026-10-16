package com.example.schedule.service;

import com.example.schedule.model.Asignatura;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.NotaFinal;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.NotaFinalRepository;
import com.example.schedule.repository.TrimestreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaFinalServiceTest {

    @Mock
    private NotaFinalRepository notaFinalRepository;
    @Mock
    private TrimestreRepository trimestreRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private CalculoNotasService calculoNotasService;

    @InjectMocks
    private NotaFinalService notaFinalService;

    private Trimestre trimestre(long id, int numero) {
        return Trimestre.builder().id(id).numero(numero).anio(2025).build();
    }

    private void stubTrimestres() {
        when(trimestreRepository.findByNumeroAndAnio(1, 2025)).thenReturn(Optional.of(trimestre(1L, 1)));
        when(trimestreRepository.findByNumeroAndAnio(2, 2025)).thenReturn(Optional.of(trimestre(2L, 2)));
        when(trimestreRepository.findByNumeroAndAnio(3, 2025)).thenReturn(Optional.of(trimestre(3L, 3)));
    }

    @Test
    void recalcularYGuardar_insertaFilaCuandoNoExiste() {
        stubTrimestres();
        when(notaFinalRepository.findByEstudianteIdAndAsignaturaIdAndAnio(5L, 2L, 2025))
                .thenReturn(Optional.empty());
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(Estudiante.builder().id(5L).build()));
        when(asignaturaRepository.findById(2L))
                .thenReturn(Optional.of(Asignatura.builder().id(2L).build()));

        when(calculoNotasService.promedioTrimestre(5L, 2L, 1L)).thenReturn(new BigDecimal("5.5"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 2L)).thenReturn(new BigDecimal("6.0"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 3L)).thenReturn(new BigDecimal("6.5"));
        when(calculoNotasService.notaFinalAnual(5L, 2L, 2025)).thenReturn(new BigDecimal("6.0"));
        when(notaFinalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notaFinalService.recalcularYGuardar(5L, 2L, 2025);

        ArgumentCaptor<NotaFinal> captor = ArgumentCaptor.forClass(NotaFinal.class);
        verify(notaFinalRepository).save(captor.capture());
        NotaFinal guardada = captor.getValue();
        assertEquals(2025, guardada.getAnio());
        assertEquals(new BigDecimal("5.5"), guardada.getNfT1());
        assertEquals(new BigDecimal("6.0"), guardada.getNfT2());
        assertEquals(new BigDecimal("6.5"), guardada.getNfT3());
        assertEquals(new BigDecimal("6.0"), guardada.getNfAnual());
    }

    @Test
    void recalcularYGuardar_actualizaFilaExistenteSinDuplicar() {
        stubTrimestres();
        NotaFinal existente = NotaFinal.builder()
                .id(10L)
                .estudiante(Estudiante.builder().id(5L).build())
                .asignatura(Asignatura.builder().id(2L).build())
                .anio(2025)
                .nfT1(new BigDecimal("4.0"))
                .build();
        when(notaFinalRepository.findByEstudianteIdAndAsignaturaIdAndAnio(5L, 2L, 2025))
                .thenReturn(Optional.of(existente));

        when(calculoNotasService.promedioTrimestre(5L, 2L, 1L)).thenReturn(new BigDecimal("5.0"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 2L)).thenReturn(new BigDecimal("5.0"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 3L)).thenReturn(new BigDecimal("5.0"));
        when(calculoNotasService.notaFinalAnual(5L, 2L, 2025)).thenReturn(new BigDecimal("5.0"));
        when(notaFinalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notaFinalService.recalcularYGuardar(5L, 2L, 2025);

        ArgumentCaptor<NotaFinal> captor = ArgumentCaptor.forClass(NotaFinal.class);
        verify(notaFinalRepository).save(captor.capture());
        NotaFinal guardada = captor.getValue();
        // Se reutiliza la misma fila (no se inserta una nueva).
        assertEquals(10L, guardada.getId());
        assertEquals(new BigDecimal("5.0"), guardada.getNfT1());
        // No se crean nuevas referencias de estudiante/asignatura.
        verify(estudianteRepository, never()).findById(any());
        verify(asignaturaRepository, never()).findById(any());
    }

    @Test
    void recalcularYGuardar_nfAnualNullCuandoFaltaUnTrimestre() {
        stubTrimestres();
        when(notaFinalRepository.findByEstudianteIdAndAsignaturaIdAndAnio(5L, 2L, 2025))
                .thenReturn(Optional.empty());
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(Estudiante.builder().id(5L).build()));
        when(asignaturaRepository.findById(2L))
                .thenReturn(Optional.of(Asignatura.builder().id(2L).build()));

        when(calculoNotasService.promedioTrimestre(5L, 2L, 1L)).thenReturn(new BigDecimal("5.5"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 2L)).thenReturn(new BigDecimal("6.0"));
        when(calculoNotasService.promedioTrimestre(5L, 2L, 3L)).thenReturn(null);
        when(calculoNotasService.notaFinalAnual(5L, 2L, 2025)).thenReturn(null);
        when(notaFinalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notaFinalService.recalcularYGuardar(5L, 2L, 2025);

        ArgumentCaptor<NotaFinal> captor = ArgumentCaptor.forClass(NotaFinal.class);
        verify(notaFinalRepository).save(captor.capture());
        NotaFinal guardada = captor.getValue();
        assertEquals(new BigDecimal("5.5"), guardada.getNfT1());
        assertEquals(new BigDecimal("6.0"), guardada.getNfT2());
        assertNull(guardada.getNfT3());
        assertNull(guardada.getNfAnual());
    }
}
