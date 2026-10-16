package com.example.schedule.service;

import com.example.schedule.dto.TrimestreResponse;
import com.example.schedule.model.EstadoTrimestre;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.TrimestreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrimestreServiceTest {

    @Mock
    private TrimestreRepository trimestreRepository;

    @InjectMocks
    private TrimestreService trimestreService;

    private Trimestre trimestre(Long id, int numero, int anio, EstadoTrimestre estado) {
        return Trimestre.builder()
                .id(id).numero(numero).anio(anio).estado(estado)
                .build();
    }

    @Test
    void abrir_cierraTrimestreAbiertoDeOtroAnio_dejandoUnoSoloAbierto() {
        // Un trimestre de año B (2024) está ABIERTO; abrimos uno del año A (2025).
        Trimestre objetivo = trimestre(1L, 1, 2025, EstadoTrimestre.CERRADO);
        Trimestre abiertoOtroAnio = trimestre(2L, 2, 2024, EstadoTrimestre.ABIERTO);

        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(objetivo));
        when(trimestreRepository.findByEstado(EstadoTrimestre.ABIERTO))
                .thenReturn(List.of(abiertoOtroAnio));
        when(trimestreRepository.save(any(Trimestre.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TrimestreResponse res = trimestreService.abrir(1L);

        // El trimestre de 2024 queda CERRADO; el objetivo queda ABIERTO.
        ArgumentCaptor<Trimestre> captor = ArgumentCaptor.forClass(Trimestre.class);
        verify(trimestreRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Trimestre otroGuardado = captor.getAllValues().stream()
                .filter(t -> t.getId().equals(2L)).findFirst().orElseThrow();
        assertEquals(EstadoTrimestre.CERRADO, otroGuardado.getEstado());
        assertEquals(EstadoTrimestre.ABIERTO.name(), res.getEstado());
        assertEquals(1L, res.getId());
    }

    @Test
    void abrir_trimestreYaAbierto_esIdempotenteSinCierresRedundantes() {
        // El único trimestre ABIERTO es el objetivo mismo: no hay cierres ni guardados redundantes.
        Trimestre objetivo = trimestre(1L, 1, 2025, EstadoTrimestre.ABIERTO);

        when(trimestreRepository.findById(1L)).thenReturn(Optional.of(objetivo));
        when(trimestreRepository.findByEstado(EstadoTrimestre.ABIERTO))
                .thenReturn(List.of(objetivo));

        TrimestreResponse res = trimestreService.abrir(1L);

        assertEquals(EstadoTrimestre.ABIERTO.name(), res.getEstado());
        // Ya estaba ABIERTO y es el único: no se guarda nada.
        verify(trimestreRepository, never()).save(any(Trimestre.class));
    }
}
