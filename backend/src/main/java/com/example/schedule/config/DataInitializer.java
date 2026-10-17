package com.example.schedule.config;

import com.example.schedule.model.*;
import com.example.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CursoRepository            cursoRepository;
    private final BloqueHorarioRepository    bloqueHorarioRepository;
    private final EspecialidadRepository     especialidadRepository;
    private final ProfesorRepository         profesorRepository;
    private final AsignaturaRepository       asignaturaRepository;
    private final CursoAsignaturaRepository  cursoAsignaturaRepository;
    private final PlanEstudioRepository      planEstudioRepository;
    private final EstudianteRepository       estudianteRepository;
    private final ApoderadoRepository        apoderadoRepository;
    private final TrimestreRepository        trimestreRepository;
    private final MatriculaRepository        matriculaRepository;
    private final com.example.schedule.repository.ParametroRepository parametroRepository;
    private final com.example.schedule.repository.ColegioRepository colegioRepository;
    private final UsuarioRepository          usuarioRepository;
    private final NotaRepository             notaRepository;
    private final NotaFinalRepository        notaFinalRepository;
    private final com.example.schedule.service.NotaFinalService notaFinalService;
    private final PasswordEncoder            passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initBloques();
        initCursos();
        initEspecialidades();
        initProfesores();
        initAsignaturas();
        initPlanesEstudio();
        initCursoAsignaturas();
        ensureAsignaturasJec();
        ensurePlanJec();
        ensureCargaJec();
        ensureJornadaRegular();
        ensureBloquesJec();
        initApoderados();
        initEstudiantes();
        initMatriculas();
        initTrimestres();
        initParametros();
        initUsuarios();
        initColegio();
        initNotasFinales();
        log.info("✅ DataInitializer completado — datos de prueba cargados.");
    }

    // ─── Usuarios (admin + un docente por profesor) ──────────────────────────

    private void initUsuarios() {
        if (!usuarioRepository.existsByUsername("admin")) {
            usuarioRepository.save(Usuario.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin"))
                .rol("ADMIN")
                .profesor(null)
                .activo(true)
                .build());
            log.info("  → Usuario admin creado (ADMIN)");
        }

        int docentes = 0;
        for (Profesor p : profesorRepository.findAll()) {
            if (p.getRut() != null && !usuarioRepository.existsByUsername(p.getRut())) {
                usuarioRepository.save(Usuario.builder()
                    .username(p.getRut())
                    .passwordHash(passwordEncoder.encode("1234"))
                    .rol("DOCENTE")
                    .profesor(p)
                    .activo(true)
                    .build());
                docentes++;
            }
        }
        log.info("  → {} usuarios DOCENTE creados (username = RUT, password = 1234)", docentes);
    }

    // ─── Bloques horarios ────────────────────────────────────────────────────

    private void initBloques() {
        if (bloqueHorarioRepository.count() > 0) return;

        Object[][] datos = {
            {1,  8,  0,  8, 45, false},
            {2,  8, 45,  9, 30, false},
            {3,  9, 30, 10, 15, false},
            {4, 10, 15, 10, 45, true },   // Recreo
            {5, 10, 45, 11, 30, false},
            {6, 11, 30, 12, 15, false},
            {7, 12, 15, 13,  0, false},
            {8, 13,  0, 13, 45, false}
        };

        for (Object[] d : datos) {
            bloqueHorarioRepository.save(BloqueHorario.builder()
                .numero((Integer) d[0])
                .horaInicio(LocalTime.of((Integer) d[1], (Integer) d[2]))
                .horaFin(LocalTime.of((Integer) d[3], (Integer) d[4]))
                .esRecreo((Boolean) d[5])
                .build());
        }
        log.info("  → 8 bloques horarios creados");
    }

    // ─── Cursos 1° a 8° Básico ───────────────────────────────────────────────

    private void initCursos() {
        if (cursoRepository.count() > 0) return;

        String[] nombres = {
            "1° Básico", "2° Básico", "3° Básico", "4° Básico",
            "5° Básico", "6° Básico", "7° Básico", "8° Básico"
        };
        for (int i = 1; i <= 8; i++) {
            cursoRepository.save(Curso.builder()
                .nombre(nombres[i - 1])
                .nivel(i)
                .build());
        }
        log.info("  → 8 cursos creados (1° a 8° Básico)");
    }

    // ─── Especialidades ──────────────────────────────────────────────────────

    private void initEspecialidades() {
        if (especialidadRepository.count() > 0) return;

        String[][] datos = {
            {"Matemáticas",        "Álgebra, geometría y aritmética"},
            {"Lenguaje",           "Comprensión lectora, escritura y gramática"},
            {"Ciencias Naturales", "Biología, química y física básica"},
            {"Historia",           "Historia, geografía y ciencias sociales"},
            {"Inglés",             "Lengua extranjera inglés"},
            {"Educación Física",   "Deportes, salud y actividad física"},
            {"Artes Visuales",     "Pintura, dibujo y expresión plástica"},
            {"Música",             "Teoría musical, canto e instrumentos"},
            {"Tecnología",         "Informática y habilidades digitales"},
            {"Religión",           "Educación religiosa y valores"}
        };

        for (String[] d : datos) {
            especialidadRepository.save(Especialidad.builder()
                .nombre(d[0])
                .descripcion(d[1])
                .build());
        }
        log.info("  → 10 especialidades creadas");
    }

    // ─── Profesores ──────────────────────────────────────────────────────────

    private void initProfesores() {
        if (profesorRepository.count() > 0) return;

        List<Especialidad> esp = especialidadRepository.findAll();
        Especialidad mat  = find(esp, "Matemáticas");
        Especialidad len  = find(esp, "Lenguaje");
        Especialidad cien = find(esp, "Ciencias Naturales");
        Especialidad hist = find(esp, "Historia");
        Especialidad ing  = find(esp, "Inglés");
        Especialidad ef   = find(esp, "Educación Física");
        Especialidad art  = find(esp, "Artes Visuales");
        Especialidad mus  = find(esp, "Música");
        Especialidad tec  = find(esp, "Tecnología");
        Especialidad rel  = find(esp, "Religión");

        // Nombre, Apellido, Email, RUT, especialidades
        Object[][] datos = {
            {"Carlos",    "García",     "c.garcia@colegio.cl",    "12.345.678-9", List.of(mat)},
            {"María",     "López",      "m.lopez@colegio.cl",     "13.456.789-0", List.of(len)},
            {"Jorge",     "Martínez",   "j.martinez@colegio.cl",  "14.567.890-1", List.of(mat, tec)},
            {"Ana",       "Rodríguez",  "a.rodriguez@colegio.cl", "15.678.901-2", List.of(cien)},
            {"Pedro",     "Soto",       "p.soto@colegio.cl",      "16.789.012-3", List.of(hist)},
            {"Claudia",   "Herrera",    "c.herrera@colegio.cl",   "17.890.123-4", List.of(ing)},
            {"Roberto",   "Fuentes",    "r.fuentes@colegio.cl",   "18.901.234-5", List.of(ef)},
            {"Patricia",  "Morales",    "p.morales@colegio.cl",   "19.012.345-6", List.of(art, mus)},
            {"Andrés",    "Vega",       "a.vega@colegio.cl",      "11.123.456-7", List.of(rel)},
            {"Valentina", "Castillo",   "v.castillo@colegio.cl",  "11.234.567-8", List.of(len, hist)},
            {"Diego",     "Núñez",      "d.nunez@colegio.cl",     "11.345.678-9", List.of(mat)},
            {"Sofía",     "Pinto",      "s.pinto@colegio.cl",     "11.456.789-0", List.of(cien, tec)},
        };

        for (Object[] d : datos) {
            @SuppressWarnings("unchecked")
            List<Especialidad> esps = (List<Especialidad>) d[4];
            profesorRepository.save(Profesor.builder()
                .nombre((String) d[0])
                .apellido((String) d[1])
                .email((String) d[2])
                .rut((String) d[3])
                .activo(true)
                .especialidades(esps)
                .build());
        }
        log.info("  → 12 profesores creados");
    }

    // ─── Asignaturas ─────────────────────────────────────────────────────────

    private void initAsignaturas() {
        if (asignaturaRepository.count() > 0) return;

        String[][] datos = {
            {"Lenguaje y Comunicación",                      "LEN", "Comprensión lectora, escritura y comunicación oral"},
            {"Matemática",                                   "MAT", "Números, álgebra, geometría y datos"},
            {"Ciencias Naturales",                           "CN",  "Biología, física y química básica"},
            {"Historia, Geografía y Ciencias Sociales",      "HIS", "Historia, geografía y formación ciudadana"},
            {"Educación Física y Salud",                     "EF",  "Actividad física, deporte y vida saludable"},
            {"Artes Visuales",                               "ART", "Expresión plástica y apreciación visual"},
            {"Música",                                       "MUS", "Expresión musical, ritmo y audición"},
            {"Orientación",                                  "ORI", "Desarrollo personal, social y afectivo"},
            {"Inglés",                                       "ING", "Lengua extranjera nivel básico"},
            {"Tecnología",                                   "TEC", "Informática y habilidades digitales"},
            {"Religión",                                     "REL", "Formación en valores y ética"},
            {"Lengua y Literatura",                          "LENLIT", "Lengua y Literatura (7° y 8° básico)"}
        };

        for (String[] d : datos) {
            asignaturaRepository.save(Asignatura.builder()
                .nombre(d[0])
                .codigo(d[1])
                .descripcion(d[2])
                .build());
        }
        log.info("  → 12 asignaturas creadas");
    }

    // ─── Planes de Estudio (malla curricular por bloque de niveles) ──────────

    private void initPlanesEstudio() {
        if (planEstudioRepository.count() > 0) return;

        List<Asignatura> asignaturas = asignaturaRepository.findAll();
        Asignatura len    = findA(asignaturas, "Lenguaje y Comunicación");
        Asignatura lenLit = findA(asignaturas, "Lengua y Literatura");
        Asignatura mat    = findA(asignaturas, "Matemática");
        Asignatura cn     = findA(asignaturas, "Ciencias Naturales");
        Asignatura his    = findA(asignaturas, "Historia, Geografía y Ciencias Sociales");
        Asignatura ef     = findA(asignaturas, "Educación Física y Salud");
        Asignatura art    = findA(asignaturas, "Artes Visuales");
        Asignatura mus    = findA(asignaturas, "Música");
        Asignatura ori    = findA(asignaturas, "Orientación");
        Asignatura ing    = findA(asignaturas, "Inglés");
        Asignatura tec    = findA(asignaturas, "Tecnología");

        // Plan 1° a 4° (decreto 439): 9 asignaturas (sin Inglés)
        PlanEstudio plan1 = planEstudioRepository.save(PlanEstudio.builder()
            .nombre("Plan de Estudio 1° a 4° Básico")
            .descripcion("Plan de Estudio de 1° a 4° Año de Educación Básica")
            .decreto("439")
            .nivelDesde(1)
            .nivelHasta(4)
            .activo(true)
            .asignaturas(new java.util.ArrayList<>(List.of(len, mat, cn, his, art, mus, ef, tec, ori)))
            .build());

        // Plan 5° a 6° (decreto 439): 10 asignaturas (+ Inglés)
        PlanEstudio plan2 = planEstudioRepository.save(PlanEstudio.builder()
            .nombre("Plan de Estudio 5° a 6° Básico")
            .descripcion("Plan de Estudio de 5° y 6° Año de Educación Básica")
            .decreto("439")
            .nivelDesde(5)
            .nivelHasta(6)
            .activo(true)
            .asignaturas(new java.util.ArrayList<>(List.of(len, mat, cn, his, art, mus, ef, tec, ori, ing)))
            .build());

        // Plan 7° a 8° (decreto 628): 10 asignaturas (usa Lengua y Literatura, no Lenguaje)
        PlanEstudio plan3 = planEstudioRepository.save(PlanEstudio.builder()
            .nombre("Plan de Estudio 7° a 8° Básico")
            .descripcion("Plan de Estudio de 7° y 8° Año de Educación Básica")
            .decreto("628")
            .nivelDesde(7)
            .nivelHasta(8)
            .activo(true)
            .asignaturas(new java.util.ArrayList<>(List.of(lenLit, ing, mat, cn, his, tec, art, mus, ef, ori)))
            .build());

        // Enlazar cada curso a su plan por nivel.
        for (Curso curso : cursoRepository.findAllByOrderByNivelAsc()) {
            int nivel = curso.getNivel();
            PlanEstudio plan = (nivel <= 4) ? plan1 : (nivel <= 6) ? plan2 : plan3;
            curso.setPlanEstudio(plan);
            cursoRepository.save(curso);
        }
        log.info("  → 3 planes de estudio creados y enlazados a los cursos");
    }

    // ─── Curso-Asignaturas (qué dicta cada profesor en cada curso) ────────────

    private void initCursoAsignaturas() {
        if (cursoAsignaturaRepository.count() > 0) return;

        List<Curso>      cursos      = cursoRepository.findAllByOrderByNivelAsc();
        List<Asignatura> asignaturas = asignaturaRepository.findAll();
        List<Profesor>   profesores  = profesorRepository.findAll();

        Asignatura len  = findA(asignaturas, "Lenguaje y Comunicación");
        Asignatura mat  = findA(asignaturas, "Matemática");
        Asignatura cn   = findA(asignaturas, "Ciencias Naturales");
        Asignatura his  = findA(asignaturas, "Historia, Geografía y Ciencias Sociales");
        Asignatura ef   = findA(asignaturas, "Educación Física y Salud");
        Asignatura art  = findA(asignaturas, "Artes Visuales");
        Asignatura mus  = findA(asignaturas, "Música");
        Asignatura ori  = findA(asignaturas, "Orientación");
        Asignatura ing  = findA(asignaturas, "Inglés");
        Asignatura tec  = findA(asignaturas, "Tecnología");
        Asignatura lenLit = findA(asignaturas, "Lengua y Literatura");

        Profesor garcia   = findP(profesores, "García");     // Matemáticas
        Profesor lopez    = findP(profesores, "López");      // Lenguaje
        Profesor martinez = findP(profesores, "Martínez");   // Matemáticas + Tecnología
        Profesor rodriguez= findP(profesores, "Rodríguez");  // Ciencias
        Profesor soto     = findP(profesores, "Soto");       // Historia
        Profesor herrera  = findP(profesores, "Herrera");    // Inglés
        Profesor fuentes  = findP(profesores, "Fuentes");    // Ed. Física
        Profesor morales  = findP(profesores, "Morales");    // Arte + Música
        Profesor vega     = findP(profesores, "Vega");       // Religión
        Profesor castillo = findP(profesores, "Castillo");   // Lenguaje + Historia
        Profesor nunez    = findP(profesores, "Núñez");      // Matemáticas
        Profesor pinto    = findP(profesores, "Pinto");      // Ciencias + Tecnología

        // Para cada curso asignamos asignaturas con horas semanales y el profesor.
        // Cursos 1°-4° usan el mismo set de profesores (carga más básica).
        // Cursos 5°-8° tienen horas adicionales de Matemáticas y Lenguaje.

        for (Curso curso : cursos) {
            // La capacidad semanal por curso es 30 bloques (6 clases/día × 5 días).
            // La suma de horas de todas las asignaturas NO debe exceder 30.
            int nivel = curso.getNivel();
            int horasLen = 6;   // Lenguaje y Comunicación
            int horasMat = 6;   // Matemática
            int horasCn  = (nivel >= 5) ? 4 : 3;   // Ciencias Naturales
            int horasHis = 3;   // Historia, Geografía y Ciencias Sociales
            int horasEf  = 2;   // Educación Física y Salud
            int horasArt = 2;   // Artes Visuales
            int horasMus = (nivel >= 5) ? 1 : 2;   // Música
            int horasOri = 1;   // Orientación
            int horasIng = (nivel >= 5) ? 3 : 2;   // Inglés (solo desde 5°, en el plan)
            int horasTec = 1;   // Tecnología (en las tres mallas, 1°-8°)

            // Reparto equilibrado: ningún profesor debe superar 30 horas/semana.
            // Hay 2 profesores de Matemáticas (garcía, martínez, núñez), 2 de Lenguaje
            // (lópez, castillo), 2 de Ciencias (rodríguez, pinto), 2 de Historia (soto, castillo).
            // Se reparten los 8 cursos equitativamente entre los profesores de cada materia.

            // Matemáticas: 3 profesores rotan los 8 cursos (≈16-18h c/u)
            Profesor profMat = switch (nivel % 3) {
                case 0 -> garcia;
                case 1 -> martinez;
                default -> nunez;
            };
            // Lenguaje: lópez toma cursos 1-4, castillo cursos 5-8 (24h c/u)
            Profesor profLen = (nivel <= 4) ? lopez : castillo;
            // Ciencias: rodríguez cursos impares, pinto cursos pares
            Profesor profCn  = (nivel % 2 == 1) ? rodriguez : pinto;
            // Historia: soto cursos 1-4, castillo cursos 5-8 — pero castillo ya tiene
            // Lenguaje 5-8, así que Historia va toda a soto para no sobrecargar.
            Profesor profHis = soto;
            // Tecnología: martínez cursos impares, pinto cursos pares
            Profesor profTec = (nivel % 2 == 1) ? martinez : pinto;

            // Lenguaje: 7°-8° usan "Lengua y Literatura" (está en su plan); 1°-6° usan "Lenguaje y Comunicación".
            Asignatura asigLenguaje = (nivel >= 7) ? lenLit : len;

            // Set base del currículum (obligatorias para todos los cursos 1°-8°).
            // Tecnología está en las tres mallas, así que es parte del set base.
            save(curso, asigLenguaje, horasLen, profLen);
            save(curso, mat,  horasMat, profMat);
            save(curso, cn,   horasCn,  profCn);
            save(curso, his,  horasHis, profHis);
            save(curso, ef,   horasEf,  fuentes);
            save(curso, art,  horasArt, morales);
            save(curso, mus,  horasMus, morales);
            save(curso, ori,  horasOri, vega);
            save(curso, tec,  horasTec, profTec);

            // Inglés: solo desde 5° (es cuando entra en el plan de estudio).
            if (nivel >= 5) {
                save(curso, ing,  horasIng, herrera);
            }
        }
        log.info("  → Curso-Asignaturas creados para los 8 cursos");
    }

    private void save(Curso curso, Asignatura asignatura, int horas, Profesor profesor) {
        cursoAsignaturaRepository.save(CursoAsignatura.builder()
            .curso(curso)
            .asignatura(asignatura)
            .horasSemanales(horas)
            .profesor(profesor)
            .build());
    }

    // ─── Siembra JEC (idempotente) para 1° a 4° Básico ───────────────────────

    // Crea las asignaturas JEC si faltan (Religión ya existe; Taller JEC es nueva).
    private void ensureAsignaturasJec() {
        ensureAsignatura("REL", "Religión", "Formación en valores y ética");
        ensureAsignatura("TAL", "Taller JEC (Libre Disposición)", "Talleres de libre disposición JEC");
    }

    private Asignatura ensureAsignatura(String codigo, String nombre, String descripcion) {
        return asignaturaRepository.findByCodigo(codigo).orElseGet(() -> {
            Asignatura a = asignaturaRepository.save(Asignatura.builder()
                .nombre(nombre)
                .codigo(codigo)
                .descripcion(descripcion)
                .build());
            log.info("  → Asignatura creada: {} ({})", nombre, codigo);
            return a;
        });
    }

    // Agrega Religión y Taller JEC al plan 1°-4° si no están (9 → 11 asignaturas).
    private void ensurePlanJec() {
        PlanEstudio plan1 = planEstudioRepository.findByNombre("Plan de Estudio 1° a 4° Básico")
            .orElseThrow(() -> new IllegalStateException("Plan 1°-4° no encontrado"));
        Asignatura rel = asignaturaRepository.findByCodigo("REL")
            .orElseThrow(() -> new IllegalStateException("Asignatura REL no encontrada"));
        Asignatura tal = asignaturaRepository.findByCodigo("TAL")
            .orElseThrow(() -> new IllegalStateException("Asignatura TAL no encontrada"));
        agregarAlPlanSiFalta(plan1, rel);
        agregarAlPlanSiFalta(plan1, tal);
    }

    private void agregarAlPlanSiFalta(PlanEstudio plan, Asignatura asignatura) {
        boolean presente = plan.getAsignaturas().stream()
            .anyMatch(a -> a.getId().equals(asignatura.getId()));
        if (!presente) {
            plan.getAsignaturas().add(asignatura);
            planEstudioRepository.save(plan);
            log.info("  → Asignatura {} agregada al plan {}", asignatura.getCodigo(), plan.getNombre());
        }
    }

    // Ajusta/crea las cargas JEC de cada curso 1°-4° (suma 38 horas pedagógicas).
    private void ensureCargaJec() {
        List<Asignatura> asignaturas = asignaturaRepository.findAll();
        List<Profesor>   profesores  = profesorRepository.findAll();

        Asignatura len  = findA(asignaturas, "Lenguaje y Comunicación");
        Asignatura mat  = findA(asignaturas, "Matemática");
        Asignatura cn   = findA(asignaturas, "Ciencias Naturales");
        Asignatura his  = findA(asignaturas, "Historia, Geografía y Ciencias Sociales");
        Asignatura ef   = findA(asignaturas, "Educación Física y Salud");
        Asignatura art  = findA(asignaturas, "Artes Visuales");
        Asignatura mus  = findA(asignaturas, "Música");
        Asignatura ori  = findA(asignaturas, "Orientación");
        Asignatura tec  = findA(asignaturas, "Tecnología");
        Asignatura rel  = findA(asignaturas, "Religión");
        Asignatura tal  = findA(asignaturas, "Taller JEC (Libre Disposición)");

        Profesor garcia   = findP(profesores, "García");
        Profesor lopez    = findP(profesores, "López");
        Profesor martinez = findP(profesores, "Martínez");
        Profesor rodriguez= findP(profesores, "Rodríguez");
        Profesor soto     = findP(profesores, "Soto");
        Profesor fuentes  = findP(profesores, "Fuentes");
        Profesor morales  = findP(profesores, "Morales");
        Profesor vega     = findP(profesores, "Vega");
        Profesor castillo = findP(profesores, "Castillo");
        Profesor nunez    = findP(profesores, "Núñez");
        Profesor pinto    = findP(profesores, "Pinto");

        for (Curso curso : cursoRepository.findAllByOrderByNivelAsc()) {
            int nivel = curso.getNivel();
            if (nivel > 4) continue;

            Profesor profMat = switch (nivel % 3) {
                case 0 -> garcia;
                case 1 -> martinez;
                default -> nunez;
            };
            Profesor profCn  = (nivel % 2 == 1) ? rodriguez : pinto;
            Profesor profTec = (nivel % 2 == 1) ? martinez : pinto;

            upsertCarga(curso, len, 8, lopez);
            upsertCarga(curso, mat, 6, profMat);
            upsertCarga(curso, tal, 6, castillo);
            upsertCarga(curso, ef,  4, fuentes);
            upsertCarga(curso, his, 3, soto);
            upsertCarga(curso, cn,  3, profCn);
            upsertCarga(curso, art, 2, morales);
            upsertCarga(curso, mus, 2, morales);
            upsertCarga(curso, rel, 2, vega);
            upsertCarga(curso, tec, 1, profTec);
            upsertCarga(curso, ori, 1, vega);
        }
        log.info("  → Cargas JEC verificadas/actualizadas para cursos 1°-4°");
    }

    // Crea la carga si falta o actualiza sus horas/profesor si cambiaron (idempotente).
    private void upsertCarga(Curso curso, Asignatura asignatura, int horas, Profesor profesor) {
        cursoAsignaturaRepository.findByCursoIdAndAsignaturaId(curso.getId(), asignatura.getId())
            .ifPresentOrElse(ca -> {
                boolean dirty = false;
                if (ca.getHorasSemanales() != horas) { ca.setHorasSemanales(horas); dirty = true; }
                if (ca.getProfesor() == null || !ca.getProfesor().getId().equals(profesor.getId())) {
                    ca.setProfesor(profesor); dirty = true;
                }
                if (dirty) cursoAsignaturaRepository.save(ca);
            }, () -> cursoAsignaturaRepository.save(CursoAsignatura.builder()
                    .curso(curso)
                    .asignatura(asignatura)
                    .horasSemanales(horas)
                    .profesor(profesor)
                    .build()));
    }

    // Backfill de la columna jornada en los bloques REGULAR preexistentes (nacen NULL con
    // ddl-auto=update). No toca id/numero/tiempos/esRecreo.
    private void ensureJornadaRegular() {
        int actualizados = 0;
        for (BloqueHorario b : bloqueHorarioRepository.findAll()) {
            if (b.getJornada() == null) {
                b.setJornada("REGULAR");
                bloqueHorarioRepository.save(b);
                actualizados++;
            }
        }
        if (actualizados > 0) {
            log.info("  → {} bloques REGULAR con jornada backfilleada", actualizados);
        }
    }

    // Siembra los 7 bloques JEC (4 clase + 3 pausas) como filas nuevas (ids ≥ 9).
    private void ensureBloquesJec() {
        if (bloqueHorarioRepository.countByJornada("JEC") > 0) return;

        // numero, horaInicioH, horaInicioM, horaFinH, horaFinM, esRecreo
        Object[][] datos = {
            {1,  8,  0,  9, 30, false},   // Bloque clase 1
            {2,  9, 45, 11, 15, false},   // Bloque clase 2
            {3, 11, 30, 13,  0, false},   // Bloque clase 3
            {4, 14, 15, 15, 45, false},   // Bloque clase 4
            {5,  9, 30,  9, 45, true },    // Recreo 1
            {6, 11, 15, 11, 30, true },    // Recreo 2
            {7, 13,  0, 14, 15, true }     // Almuerzo
        };
        for (Object[] d : datos) {
            bloqueHorarioRepository.save(BloqueHorario.builder()
                .numero((Integer) d[0])
                .horaInicio(LocalTime.of((Integer) d[1], (Integer) d[2]))
                .horaFin(LocalTime.of((Integer) d[3], (Integer) d[4]))
                .esRecreo((Boolean) d[5])
                .jornada("JEC")
                .build());
        }
        log.info("  → 7 bloques JEC creados (4 clase + 3 pausas)");
    }

    // ─── Apoderados ──────────────────────────────────────────────────────────

    private void initApoderados() {
        if (apoderadoRepository.count() > 0) return;

        // RUT, nombre, apellidoPaterno, dirección, celular
        Object[][] datos = {
            {"9.111.222-3",  "Rosa",     "Fernández", "Los Aromos 123",  "+56912345678"},
            {"10.222.333-4", "Manuel",   "Soto",      "Las Encinas 456", "+56923456789"},
            {"11.333.444-5", "Carolina", "Araya",     "El Peñón 789",    "+56934567890"},
        };

        for (Object[] d : datos) {
            apoderadoRepository.save(Apoderado.builder()
                .rut((String) d[0])
                .nombre((String) d[1])
                .apellidoPaterno((String) d[2])
                .region("Región Metropolitana")
                .comuna("La Reina")
                .direccion((String) d[3])
                .telefonoCelular((String) d[4])
                .correo("aespinoza3010@gmail.com")
                .build());
        }
        log.info("  → 3 apoderados creados");
    }

    // ─── Estudiantes (5 por curso = 40 en total) ─────────────────────────────

    private void initEstudiantes() {
        if (estudianteRepository.count() > 0) return;

        List<Curso> cursos = cursoRepository.findAllByOrderByNivelAsc();
        List<Apoderado> apoderados = apoderadoRepository.findAll();

        // Nombres y apellidos chilenos de ejemplo
        String[] nombres   = {"Matías", "Sofía", "Benjamín", "Valentina", "Lucas",
                               "Isidora", "Martín", "Javiera", "Diego", "Camila"};
        String[] apellidosP = {"González", "Muñoz", "Rojas", "Díaz", "Pérez",
                                "Sánchez", "Vargas", "Castro", "Flores", "Torres"};
        String[] apellidosM = {"Silva", "Mora", "Reyes", "Ortiz", "Ramos",
                                "Cruz", "Lara", "Espinoza", "Ríos", "Parra"};

        int rutBase = 20_000_000;
        int estudianteIdx = 0;

        for (Curso curso : cursos) {
            int anioNac = 2025 - (5 + curso.getNivel()); // edad aproximada por nivel
            for (int i = 0; i < 5; i++) {
                String nombre    = nombres[estudianteIdx % nombres.length];
                String apellidoP = apellidosP[estudianteIdx % apellidosP.length];
                String apellidoM = apellidosM[estudianteIdx % apellidosM.length];
                int    dv        = rutBase % 11;
                String rutStr    = formatRut(rutBase) + "-" + (dv == 10 ? "K" : dv);
                LocalDate fnac   = LocalDate.of(anioNac, (i % 12) + 1, (i * 5 + 3));

                // Asigna un apoderado a algunos estudiantes (los primeros 2 de cada curso).
                Apoderado apoderado = (!apoderados.isEmpty() && i < 2)
                    ? apoderados.get(estudianteIdx % apoderados.size())
                    : null;

                Estudiante.EstudianteBuilder builder = Estudiante.builder()
                    .nombre(nombre)
                    .apellidoPaterno(apellidoP)
                    .apellidoMaterno(apellidoM)
                    .rut(rutStr)
                    .fechaNacimiento(fnac)
                    .curso(curso)
                    .apoderado(apoderado);

                // Datos de muestra de la ficha para los primeros 2 alumnos de cada curso.
                // foto se deja en null. Sexo alterna Masculino/Femenino.
                if (i < 2) {
                    builder.nacionalidad("Chilena")
                           .sexo(i % 2 == 0 ? "Masculino" : "Femenino")
                           .comuna("Santiago")
                           .prevision("Fonasa");
                }

                estudianteRepository.save(builder.build());

                rutBase++;
                estudianteIdx++;
            }
        }
        log.info("  → 40 estudiantes creados (5 por curso)");
    }

    // ─── Trimestres (3 del año actual, CERRADOS por defecto) ─────────────────

    private void initTrimestres() {
        if (trimestreRepository.count() > 0) return;

        int anio = Year.now().getValue();
        // Todos los trimestres se crean CERRADOS por defecto.
        // El usuario abre manualmente el que esté en curso (solo uno a la vez).
        for (int numero = 1; numero <= 3; numero++) {
            trimestreRepository.save(Trimestre.builder()
                .numero(numero)
                .anio(anio)
                .estado(EstadoTrimestre.CERRADO)
                .build());
        }
        log.info("  → 3 trimestres creados (CERRADOS) para el año {}", anio);
    }

    // ─── Colegio (datos institucionales + logo) ──────────────────────────────

    private void initColegio() {
        if (colegioRepository.count() > 0) return;

        // Logo del colegio: escudo "LS" en PNG generado en memoria (data URI Base64).
        // PNG para que OpenPDF pueda incrustarlo en el reporte (no soporta SVG).
        String logoDataUri = ColegioLogo.generarDataUri();

        colegioRepository.save(com.example.schedule.model.Colegio.builder()
            .nombre("Colegio De La Salle")
            .region("Región Metropolitana")
            .provincia("Santiago")
            .comuna("La Reina")
            .direccion("Avenida Ossa 1349")
            .rbd("12345-6")
            .decreto("Decreto Exento N° 2960/2007")
            .logo(logoDataUri)
            .build());
        log.info("  → Colegio creado: Colegio De La Salle");
    }

    // ─── Parámetros (aproxNota) ──────────────────────────────────────────────

    private void initParametros() {
        int anioActual = Year.now().getValue();
        Parametro p = parametroRepository.findTopByOrderByIdAsc().orElse(null);
        if (p == null) {
            // Por defecto 'S' (aproxima la nota), 60 segundos de inactividad, año escolar = actual
            // y los parámetros de jornada JEC.
            parametroRepository.save(Parametro.builder()
                .aproxNota("S")
                .timeoutInactividad(60)
                .anioEscolarActivo(anioActual)
                .tipoJornada("JEC")
                .duracionRecreo(15)
                .horaInicioJornada("08:00")
                .duracionBloqueMin(90)
                .duracionAlmuerzo(75)
                .build());
            log.info("  → Parámetro creado: aproxNota='S', timeoutInactividad=60s, anioEscolarActivo={}, jornada JEC", anioActual);
        } else {
            // Backfill incondicional (independiente del guard de anioEscolarActivo): los campos
            // JEC nacen NULL en BD poblada con ddl-auto=update y deben quedar con su default.
            boolean dirty = false;
            if (p.getAnioEscolarActivo() == null) { p.setAnioEscolarActivo(anioActual); dirty = true; }
            if (p.getTipoJornada() == null)       { p.setTipoJornada("JEC");            dirty = true; }
            if (p.getDuracionRecreo() == null)    { p.setDuracionRecreo(15);            dirty = true; }
            if (p.getHoraInicioJornada() == null) { p.setHoraInicioJornada("08:00");    dirty = true; }
            if (p.getDuracionBloqueMin() == null) { p.setDuracionBloqueMin(90);         dirty = true; }
            if (p.getDuracionAlmuerzo() == null)  { p.setDuracionAlmuerzo(75);          dirty = true; }
            if (dirty) {
                parametroRepository.save(p);
                log.info("  → Parámetro: backfill de campos nulos (anioEscolarActivo/JEC)");
            }
        }
    }

    // ─── Matrículas (año actual para los estudiantes sembrados) ──────────────

    private void initMatriculas() {
        if (matriculaRepository.count() > 0) return;

        int anio = Year.now().getValue();
        int creadas = 0;
        for (Estudiante estudiante : estudianteRepository.findAll()) {
            if (estudiante.getCurso() == null) continue;
            matriculaRepository.save(Matricula.builder()
                .estudiante(estudiante)
                .curso(estudiante.getCurso())
                .anio(anio)
                .estado(EstadoMatricula.ACTIVA)
                .build());
            creadas++;
        }
        log.info("  → {} matrículas creadas (año {})", creadas, anio);
    }

    // ─── Notas finales persistidas (backfill desde las notas existentes) ─────

    private void initNotasFinales() {
        if (notaFinalRepository.count() > 0) return;

        // Deriva las combinaciones distintas (estudiante, asignatura, año) a partir de
        // las notas existentes y persiste sus notas finales en la tabla 'nota_final'.
        java.util.Set<String> procesadas = new java.util.HashSet<>();
        int creadas = 0;
        for (Nota nota : notaRepository.findAll()) {
            Long estudianteId = nota.getEstudiante().getId();
            Long asignaturaId = nota.getAsignatura().getId();
            int anio = nota.getTrimestre().getAnio();
            String clave = estudianteId + "|" + asignaturaId + "|" + anio;
            if (procesadas.add(clave)) {
                notaFinalService.recalcularYGuardar(estudianteId, asignaturaId, anio);
                creadas++;
            }
        }
        log.info("  → {} notas finales persistidas (backfill)", creadas);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Especialidad find(List<Especialidad> list, String nombre) {
        return list.stream()
            .filter(e -> e.getNombre().equals(nombre))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Especialidad no encontrada: " + nombre));
    }

    private Asignatura findA(List<Asignatura> list, String nombre) {
        return list.stream()
            .filter(a -> a.getNombre().equals(nombre))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Asignatura no encontrada: " + nombre));
    }

    private Profesor findP(List<Profesor> list, String apellido) {
        return list.stream()
            .filter(p -> p.getApellido().equals(apellido))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Profesor no encontrado: " + apellido));
    }

    private String formatRut(int rut) {
        String s = String.valueOf(rut);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ".");
            sb.insert(0, s.charAt(i));
            count++;
        }
        return sb.toString();
    }
}
