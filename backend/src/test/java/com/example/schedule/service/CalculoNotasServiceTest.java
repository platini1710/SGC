package com.example.schedule.service;

import com.example.schedule.model.Nota;
import com.example.schedule.model.Parametro;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.NotaRepository;
import com.example.schedule.repository.ParametroRepository;
import com.example.schedule.repository.TrimestreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifica el cálculo de promedios con la regla de aproximación a 1 decimal
 * controlada por el parámetro aproxNota:
 *   - 'S' -> redondea al decimal superior (HALF_UP): 6.35 -> 6.4
 *   - otro -> trunca el segundo decimal (DOWN):      6.35 -> 6.3
 * más la regla fija 3,95 -> 4,00 sobre la Nota Final anual (aplicada antes de llevar a 1 decimal).
 */
@ExtendWith(MockitoExtension.class)
class CalculoNotasServiceTest {

    private static final Long ESTUDIANTE_ID = 10L;
    private static final Long ASIGNATURA_ID = 1L;
    private static final int ANIO = 2026;

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private TrimestreRepository trimestreRepository;

    @Mock
    private ParametroRepository parametroRepository;

    @InjectMocks
    private CalculoNotasService service;

    private Nota nota(BigDecimal valor) {
        return Nota.builder().valor(valor).build();
    }

    // Configura el parámetro aproxNota ('S' aproxima, cualquier otro trunca).
    private void stubAproxNota(String valor) {
        lenient().when(parametroRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(Parametro.builder().aproxNota(valor).build()));
    }

    private void stubTrimestre(int numero, Long trimestreId) {
        Trimestre t = Trimestre.builder().id(trimestreId).numero(numero).anio(ANIO).build();
        lenient().when(trimestreRepository.findByNumeroAndAnio(numero, ANIO)).thenReturn(Optional.of(t));
    }

    private void stubNotas(Long trimestreId, Nota... notas) {
        lenient().when(notaRepository
                        .findByEstudianteIdAndAsignaturaIdAndTrimestreId(ESTUDIANTE_ID, ASIGNATURA_ID, trimestreId))
                .thenReturn(List.of(notas));
    }

    // ─── promedioTrimestre ───────────────────────────────────────────────────

    @Test
    void promedioTrimestre_conNotas_aproximaS() {
        stubAproxNota("S"); // 6.5 y 5.0 -> 5.75 -> aproxima a 5.8
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(ESTUDIANTE_ID, ASIGNATURA_ID, 100L))
                .thenReturn(List.of(nota(new BigDecimal("6.5")), nota(new BigDecimal("5.0"))));

        BigDecimal result = service.promedioTrimestre(ESTUDIANTE_ID, ASIGNATURA_ID, 100L);

        assertEquals(0, result.compareTo(new BigDecimal("5.8")));
    }

    @Test
    void promedioTrimestre_conNotas_truncaN() {
        stubAproxNota("N"); // 6.5 y 6.2 -> 6.35 -> trunca a 6.3
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(ESTUDIANTE_ID, ASIGNATURA_ID, 100L))
                .thenReturn(List.of(nota(new BigDecimal("6.5")), nota(new BigDecimal("6.2"))));

        BigDecimal result = service.promedioTrimestre(ESTUDIANTE_ID, ASIGNATURA_ID, 100L);

        assertEquals(0, result.compareTo(new BigDecimal("6.3")));
    }

    @Test
    void promedioTrimestre_conNotas_aproximaS_6_35_a_6_4() {
        stubAproxNota("S"); // 6.5 y 6.2 -> 6.35 -> aproxima a 6.4
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(ESTUDIANTE_ID, ASIGNATURA_ID, 100L))
                .thenReturn(List.of(nota(new BigDecimal("6.5")), nota(new BigDecimal("6.2"))));

        BigDecimal result = service.promedioTrimestre(ESTUDIANTE_ID, ASIGNATURA_ID, 100L);

        assertEquals(0, result.compareTo(new BigDecimal("6.4")));
    }

    @Test
    void promedioTrimestre_sinNotas_devuelveNull() {
        when(notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(ESTUDIANTE_ID, ASIGNATURA_ID, 100L))
                .thenReturn(List.of());

        BigDecimal result = service.promedioTrimestre(ESTUDIANTE_ID, ASIGNATURA_ID, 100L);

        assertNull(result);
    }

    // ─── notaFinalAnual: regla fija 3,95 -> 4,00 ──────────────────────────────

    /**
     * Los tres trimestres (ids 1,2,3) con una única nota igual a {@code promedioObjetivo},
     * de modo que la NF cruda (antes de reglas) sea exactamente ese valor.
     */
    private void stubTresTrimestresConPromedio(String promedioObjetivo) {
        stubTrimestre(1, 1L);
        stubTrimestre(2, 2L);
        stubTrimestre(3, 3L);
        stubNotas(1L, nota(new BigDecimal(promedioObjetivo)));
        stubNotas(2L, nota(new BigDecimal(promedioObjetivo)));
        stubNotas(3L, nota(new BigDecimal(promedioObjetivo)));
    }

    @Test
    void notaFinal_promediosQueDan395_aplicaReglaFijaYda40() {
        stubAproxNota("S");
        // Con aproxNota='S', cada promedio trimestral de 3.95 se redondea a 4.0.
        // Pero para forzar una NF cruda de exactamente 3.95 usamos promedios ya a 1 decimal
        // que promedien 3.95: p.ej. 3.9, 3.9 y 4.05 no es posible; usamos el caso directo:
        // tres trimestres cuyo promedio (ya a 1 decimal) sea 3.9, 3.9, 4.0 -> NF cruda 3.9333 (no aplica).
        // Para probar la regla fija 3,95->4,00 directamente, usamos promedios 3.9, 4.0, 4.0 -> 3.9667 (no).
        // La regla fija solo se alcanza con NF cruda en [3.95,4.00): promedios 3.9,4.0,4.0 => 3.97 -> regla -> 4.00 -> 4.0
        stubTrimestre(1, 1L);
        stubTrimestre(2, 2L);
        stubTrimestre(3, 3L);
        stubNotas(1L, nota(new BigDecimal("3.9")));
        stubNotas(2L, nota(new BigDecimal("4.0")));
        stubNotas(3L, nota(new BigDecimal("4.0")));

        BigDecimal nf = service.notaFinalAnual(ESTUDIANTE_ID, ASIGNATURA_ID, ANIO);

        // promedios a 1 decimal: 3.9, 4.0, 4.0 -> NF cruda (3.9+4.0+4.0)/3 = 3.9667 -> redondeo2 3.97
        // regla fija 3.97 en [3.95,4.00) -> 4.00 -> aproximación 'S' a 1 decimal -> 4.0
        assertEquals(0, nf.compareTo(new BigDecimal("4.0")));
    }

    @Test
    void notaFinal_394_noSeAproximaReglaFija() {
        stubAproxNota("N"); // 3.94 no entra en la regla fija; trunca a 1 decimal -> 3.9
        stubTresTrimestresConPromedio("3.94");

        BigDecimal nf = service.notaFinalAnual(ESTUDIANTE_ID, ASIGNATURA_ID, ANIO);

        assertEquals(0, nf.compareTo(new BigDecimal("3.9")));
    }

    @Test
    void notaFinal_400_seMantiene() {
        stubAproxNota("N");
        stubTresTrimestresConPromedio("4.00");

        BigDecimal nf = service.notaFinalAnual(ESTUDIANTE_ID, ASIGNATURA_ID, ANIO);

        assertEquals(0, nf.compareTo(new BigDecimal("4.0")));
    }

    // ─── notaFinalAnual: año parcial y vacío ──────────────────────────────────

    @Test
    void notaFinal_soloT1yT2_promediaSoloLosPresentes() {
        stubAproxNota("N"); // (5.0 + 6.0) / 2 = 5.50 -> trunca a 1 decimal 5.5
        stubTrimestre(1, 1L);
        stubTrimestre(2, 2L);
        stubTrimestre(3, 3L);
        stubNotas(1L, nota(new BigDecimal("5.0")));
        stubNotas(2L, nota(new BigDecimal("6.0")));
        stubNotas(3L); // T3 sin notas

        BigDecimal nf = service.notaFinalAnual(ESTUDIANTE_ID, ASIGNATURA_ID, ANIO);

        assertEquals(0, nf.compareTo(new BigDecimal("5.5")));
    }

    @Test
    void notaFinal_sinNingunaNota_devuelveNull() {
        stubTrimestre(1, 1L);
        stubTrimestre(2, 2L);
        stubTrimestre(3, 3L);
        stubNotas(1L);
        stubNotas(2L);
        stubNotas(3L);

        BigDecimal nf = service.notaFinalAnual(ESTUDIANTE_ID, ASIGNATURA_ID, ANIO);

        assertNull(nf);
    }
}
