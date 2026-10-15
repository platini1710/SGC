package com.example.schedule.service;

import com.example.schedule.dto.ConsolidadoAnualResponse;
import com.example.schedule.dto.NotaResponse;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.TrimestreRepository;
import com.example.schedule.security.AuditoriaHelper;
import com.example.schedule.security.NotaAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private NotaService notaService;
    @Mock
    private NotaAccessChecker notaAccessChecker;
    @Mock
    private AuditoriaHelper auditoriaHelper;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private TrimestreRepository trimestreRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void mapearNotas_posicionesFaltantes_sinEntrada() {
        List<NotaResponse> notas = List.of(
                NotaResponse.builder().posicion(1).valor(new BigDecimal("5.5")).build(),
                NotaResponse.builder().posicion(3).valor(new BigDecimal("3.2")).build());

        Map<Integer, BigDecimal> mapa = reporteService.mapearNotas(notas);

        assertEquals(new BigDecimal("5.5"), mapa.get(1));
        assertEquals(new BigDecimal("3.2"), mapa.get(3));
        assertNull(mapa.get(2), "La posición ausente no debe tener valor (se rinde '-')");
        assertNull(mapa.get(9));
    }

    @Test
    void mapearNotas_listaNula_devuelveMapaVacio() {
        assertTrue(reporteService.mapearNotas(null).isEmpty());
    }

    @Test
    void generarPdf_verificaAccesoAntesDeConstruir() {
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");
        lenient().when(colegioRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        lenient().when(cursoRepository.findById(anyLong())).thenReturn(Optional.empty());
        lenient().when(asignaturaRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(notaService.getConsolidado(anyLong(), anyLong(), anyInt()))
                .thenReturn(ConsolidadoAnualResponse.builder().filas(List.of()).build());

        byte[] pdf = reporteService.generarPdf(1L, 2L, null);

        verify(notaAccessChecker).verificar(1L, 2L);
        assertTrue(pdf.length > 0, "El PDF debe generarse aun sin notas");
    }
}
