package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Parametro;
import com.example.schedule.repository.ParametroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnioEscolarServiceTest {

    @Mock
    private ParametroRepository parametroRepository;

    @InjectMocks
    private AnioEscolarService anioEscolarService;

    @Test
    void resolver_valorPresenteEnRango_devuelveEseValor() {
        // No debe consultar el parámetro cuando el valor viene explícito.
        assertEquals(2024, anioEscolarService.resolver(2024));
    }

    @Test
    void resolver_valorNull_conParametro_devuelveAnioEscolarActivo() {
        when(parametroRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(Parametro.builder().anioEscolarActivo(2023).build()));

        assertEquals(2023, anioEscolarService.resolver(null));
    }

    @Test
    void resolver_valorNull_sinParametro_devuelveAnioActual() {
        when(parametroRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        assertEquals(Year.now().getValue(), anioEscolarService.resolver(null));
    }

    @Test
    void resolver_valorNull_parametroSinAnio_devuelveAnioActual() {
        lenient().when(parametroRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(Parametro.builder().anioEscolarActivo(null).build()));

        assertEquals(Year.now().getValue(), anioEscolarService.resolver(null));
    }

    @Test
    void validarRango_bajoLimite_lanzaSchedulingException() {
        assertThrows(SchedulingException.class, () -> anioEscolarService.validarRango(1999));
    }

    @Test
    void validarRango_sobreLimite_lanzaSchedulingException() {
        assertThrows(SchedulingException.class, () -> anioEscolarService.validarRango(2101));
    }

    @Test
    void validarRango_dentroDeRango_noLanza() {
        anioEscolarService.validarRango(2000);
        anioEscolarService.validarRango(2100);
    }
}
