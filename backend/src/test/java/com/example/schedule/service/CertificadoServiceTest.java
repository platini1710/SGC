package com.example.schedule.service;

import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.security.NotaAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private CalculoNotasService calculoNotasService;
    @Mock
    private NotaAccessChecker notaAccessChecker;

    @InjectMocks
    private CertificadoService certificadoService;

    @Test
    void notaEnPalabras_sieteCero() {
        assertEquals("siete coma cero", certificadoService.notaEnPalabras(new BigDecimal("7.0")));
    }

    @Test
    void notaEnPalabras_seisCuatro() {
        assertEquals("seis coma cuatro", certificadoService.notaEnPalabras(new BigDecimal("6.4")));
    }

    @Test
    void notaEnPalabras_cuatroCero() {
        assertEquals("cuatro coma cero", certificadoService.notaEnPalabras(new BigDecimal("4.0")));
    }

    @Test
    void notaEnPalabras_unoCero() {
        assertEquals("uno coma cero", certificadoService.notaEnPalabras(new BigDecimal("1.0")));
    }

    @Test
    void notaEnPalabras_cincoCinco() {
        assertEquals("cinco coma cinco", certificadoService.notaEnPalabras(new BigDecimal("5.5")));
    }

    @Test
    void notaEnPalabras_null_devuelveGuion() {
        assertEquals("-", certificadoService.notaEnPalabras(null));
    }
}
