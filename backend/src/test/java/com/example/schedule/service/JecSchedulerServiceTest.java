package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import com.example.schedule.security.AuditoriaHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JecSchedulerServiceTest {

    @Mock private HorarioRepository horarioRepository;
    @Mock private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock private ParametroRepository parametroRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private AuditoriaHelper auditoriaHelper;

    @InjectMocks private JecSchedulerService jecSchedulerService;

    private Asignatura asig(long id, String nombre, String codigo) {
        return Asignatura.builder().id(id).nombre(nombre).codigo(codigo).build();
    }

    private Profesor prof(long id, String apellido) {
        return Profesor.builder().id(id).nombre("P" + id).apellido(apellido).build();
    }

    private BloqueHorario bloqueJec(long id, int numero) {
        return BloqueHorario.builder().id(id).numero(numero).esRecreo(false).jornada("JEC").build();
    }

    private CursoAsignatura carga(Curso c, Asignatura a, Profesor p, int horas) {
        return CursoAsignatura.builder().curso(c).asignatura(a).profesor(p).horasSemanales(horas).build();
    }

    // Carga completa del plan 1°-4° (38 horas pedagógicas) para un curso.
    private List<CursoAsignatura> cargaPlanCompleto(Curso c) {
        Profesor lopez = prof(100, "López");
        Profesor mat = prof(101, "García");
        Profesor tal = prof(102, "Castillo");
        Profesor ef = prof(103, "Fuentes");
        Profesor soto = prof(104, "Soto");
        Profesor cn = prof(105, "Rodríguez");
        Profesor morales = prof(106, "Morales");
        Profesor vega = prof(107, "Vega");
        Profesor tec = prof(108, "Martínez");
        return new ArrayList<>(List.of(
                carga(c, asig(10, "Lenguaje y Comunicación", "LEN"), lopez, 8),
                carga(c, asig(11, "Matemática", "MAT"), mat, 6),
                carga(c, asig(12, "Taller JEC (Libre Disposición)", "TAL"), tal, 6),
                carga(c, asig(13, "Educación Física y Salud", "EF"), ef, 4),
                carga(c, asig(14, "Historia, Geografía y Ciencias Sociales", "HIS"), soto, 3),
                carga(c, asig(15, "Ciencias Naturales", "CN"), cn, 3),
                carga(c, asig(16, "Artes Visuales", "ART"), morales, 2),
                carga(c, asig(17, "Música", "MUS"), morales, 2),
                carga(c, asig(18, "Religión", "REL"), vega, 2),
                carga(c, asig(19, "Tecnología", "TEC"), tec, 1),
                carga(c, asig(20, "Orientación", "ORI"), vega, 1)));
    }

    @Test
    void computaTiemposBloqueConDefaults() {
        Parametro p = Parametro.builder().build(); // defaults de @Builder.Default
        List<JecSchedulerService.BloqueTiempo> t = jecSchedulerService.computarTiemposBloque(p);

        assertEquals(4, t.size());
        assertEquals(LocalTime.of(8, 0), t.get(0).inicio());
        assertEquals(LocalTime.of(9, 30), t.get(0).fin());
        assertEquals(LocalTime.of(9, 45), t.get(1).inicio());
        assertEquals(LocalTime.of(11, 15), t.get(1).fin());
        assertEquals(LocalTime.of(11, 30), t.get(2).inicio());
        assertEquals(LocalTime.of(13, 0), t.get(2).fin());
        assertEquals(LocalTime.of(14, 15), t.get(3).inicio());
        assertEquals(LocalTime.of(15, 45), t.get(3).fin());
    }

    @Test
    void computaTiemposBloqueNullSafe() {
        // Parámetro nulo → usa todos los defaults sin NPE.
        List<JecSchedulerService.BloqueTiempo> t = jecSchedulerService.computarTiemposBloque(null);
        assertEquals(LocalTime.of(8, 0), t.get(0).inicio());
        assertEquals(LocalTime.of(14, 15), t.get(3).inicio());
    }

    @Test
    void generaDiecinueveBloquesPorCursoConDistribucionSemanal() {
        Curso curso = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        List<BloqueHorario> bloques = List.of(
                bloqueJec(9, 1), bloqueJec(10, 2), bloqueJec(11, 3), bloqueJec(12, 4));

        when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC")).thenReturn(bloques);
        when(cursoRepository.findAllByOrderByNivelAsc()).thenReturn(List.of(curso));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(cargaPlanCompleto(curso));
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");

        List<Horario> solucion = jecSchedulerService.colocarCursosJec();

        // 19 slots por semana.
        assertEquals(19, solucion.size());

        // Lun–Jue = 4, Viernes = 3.
        Map<Integer, Long> porDia = new HashMap<>();
        for (Horario h : solucion) {
            porDia.merge(h.getDiaSemana(), 1L, Long::sum);
        }
        assertEquals(4L, porDia.get(1));
        assertEquals(4L, porDia.get(2));
        assertEquals(4L, porDia.get(3));
        assertEquals(4L, porDia.get(4));
        assertEquals(3L, porDia.get(5));
    }

    @Test
    void cadaAsignaturaRecibeSuConteoDeBloques() {
        Curso curso = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        List<BloqueHorario> bloques = List.of(
                bloqueJec(9, 1), bloqueJec(10, 2), bloqueJec(11, 3), bloqueJec(12, 4));

        when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC")).thenReturn(bloques);
        when(cursoRepository.findAllByOrderByNivelAsc()).thenReturn(List.of(curso));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(cargaPlanCompleto(curso));
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");

        List<Horario> solucion = jecSchedulerService.colocarCursosJec();

        // Cuenta de apariciones de cada asignatura (primaria + secundaria).
        Map<String, Integer> apariciones = new HashMap<>();
        for (Horario h : solucion) {
            apariciones.merge(h.getAsignatura().getCodigo(), 1, Integer::sum);
            if (h.getAsignaturaSecundaria() != null) {
                apariciones.merge(h.getAsignaturaSecundaria().getCodigo(), 1, Integer::sum);
            }
        }
        assertEquals(4, apariciones.get("LEN"));
        assertEquals(3, apariciones.get("MAT"));
        assertEquals(3, apariciones.get("TAL"));
        assertEquals(2, apariciones.get("EF"));
        assertEquals(2, apariciones.get("HIS")); // 1 full + 1 medio
        assertEquals(2, apariciones.get("CN"));  // 1 full + 1 medio
        assertEquals(1, apariciones.get("ART"));
        assertEquals(1, apariciones.get("MUS"));
        assertEquals(1, apariciones.get("REL"));
        assertEquals(1, apariciones.get("TEC")); // medio
        assertEquals(1, apariciones.get("ORI")); // medio
    }

    @Test
    void emparejaExactamenteDosMediosBloquesCompartidos() {
        Curso curso = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        List<BloqueHorario> bloques = List.of(
                bloqueJec(9, 1), bloqueJec(10, 2), bloqueJec(11, 3), bloqueJec(12, 4));

        when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC")).thenReturn(bloques);
        when(cursoRepository.findAllByOrderByNivelAsc()).thenReturn(List.of(curso));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(cargaPlanCompleto(curso));
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");

        List<Horario> solucion = jecSchedulerService.colocarCursosJec();

        List<Horario> medios = solucion.stream()
                .filter(h -> Boolean.TRUE.equals(h.getEsMedioBloque())).toList();
        assertEquals(2, medios.size());
        for (Horario m : medios) {
            assertNotNull(m.getAsignaturaSecundaria());
            assertNotNull(m.getProfesorSecundaria());
        }
        // Las parejas preferentes son {CN,TEC} y {HIS,ORI}.
        for (Horario m : medios) {
            String a = m.getAsignatura().getCodigo();
            String b = m.getAsignaturaSecundaria().getCodigo();
            boolean cnTec = (a.equals("CN") && b.equals("TEC")) || (a.equals("TEC") && b.equals("CN"));
            boolean hisOri = (a.equals("HIS") && b.equals("ORI")) || (a.equals("ORI") && b.equals("HIS"));
            assertTrue(cnTec || hisOri, "Pareja inesperada: " + a + "/" + b);
        }
    }

    @Test
    void ningunProfesorEstaDobleAgendadoIncluyendoSubtramoSecundario() {
        // Dos cursos 1° y 2° comparten profesores → la colisión debe respetarse en memoria.
        Curso c1 = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        Curso c2 = Curso.builder().id(2L).nombre("2° Básico").nivel(2).build();
        List<BloqueHorario> bloques = List.of(
                bloqueJec(9, 1), bloqueJec(10, 2), bloqueJec(11, 3), bloqueJec(12, 4));

        when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC")).thenReturn(bloques);
        when(cursoRepository.findAllByOrderByNivelAsc()).thenReturn(List.of(c1, c2));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(cargaPlanCompleto(c1));
        when(cursoAsignaturaRepository.findByCursoId(2L)).thenReturn(cargaPlanCompleto(c2));
        when(auditoriaHelper.usuarioActual()).thenReturn("tester");

        List<Horario> solucion = jecSchedulerService.colocarCursosJec();

        java.util.Set<String> ocupacion = new java.util.HashSet<>();
        for (Horario h : solucion) {
            String k1 = h.getProfesor().getId() + "-" + h.getDiaSemana() + "-" + h.getBloque().getId();
            assertTrue(ocupacion.add(k1), "Profesor primario doble-agendado: " + k1);
            if (h.getProfesorSecundaria() != null) {
                String k2 = h.getProfesorSecundaria().getId() + "-" + h.getDiaSemana() + "-" + h.getBloque().getId();
                assertTrue(ocupacion.add(k2), "Profesor secundario doble-agendado: " + k2);
            }
        }
    }

    @Test
    void cargaImparDeMediosLanzaSchedulingException() {
        // Un solo medio (Tecnología=1) sin pareja → número impar de medios.
        Curso curso = Curso.builder().id(1L).nombre("1° Básico").nivel(1).build();
        Profesor tec = prof(108, "Martínez");
        List<CursoAsignatura> cargas = new ArrayList<>(List.of(
                carga(curso, asig(19, "Tecnología", "TEC"), tec, 1)));
        List<BloqueHorario> bloques = List.of(
                bloqueJec(9, 1), bloqueJec(10, 2), bloqueJec(11, 3), bloqueJec(12, 4));

        lenient().when(bloqueHorarioRepository.findByJornadaAndEsRecreoFalseOrderByNumeroAsc("JEC")).thenReturn(bloques);
        when(cursoRepository.findAllByOrderByNivelAsc()).thenReturn(List.of(curso));
        when(cursoAsignaturaRepository.findByCursoId(1L)).thenReturn(cargas);

        assertThrows(SchedulingException.class, () -> jecSchedulerService.colocarCursosJec());
    }
}
