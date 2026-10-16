package com.example.schedule.service;

import com.example.schedule.dto.PromocionDecisionDTO;
import com.example.schedule.dto.PromocionConfirmRequest;
import com.example.schedule.dto.PromocionItemDTO;
import com.example.schedule.dto.PromocionPreviewResponse;
import com.example.schedule.dto.PromocionResultadoResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.AccionPromocion;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoMatricula;
import com.example.schedule.model.EstadoTrimestre;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
import com.example.schedule.repository.TrimestreRepository;
import com.example.schedule.security.AuditoriaHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final EstudianteRepository estudianteRepository;
    private final AnioEscolarService anioEscolarService;
    private final TrimestreService trimestreService;
    private final TrimestreRepository trimestreRepository;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Previsualiza la promoción masiva de {@code anioOrigen} a {@code anioDestino}.
     * NO escribe nada: solo propone, por alumno, la acción (PROMOVER / EGRESA / SIN_CURSO_DESTINO).
     */
    public PromocionPreviewResponse previewPromocion(Integer anioOrigen, Integer anioDestino) {
        validarAnios(anioOrigen, anioDestino);
        validarTrimestresCerrados(anioOrigen);

        int nivelMax = cursoRepository.findAllByOrderByNivelAsc().stream()
                .filter(c -> c.getNivel() != null)
                .mapToInt(Curso::getNivel)
                .max()
                .orElse(0);

        List<PromocionItemDTO> items = new ArrayList<>();
        List<String> incidencias = new ArrayList<>();

        for (Matricula origen : matriculaRepository.findByAnioAndEstado(anioOrigen, EstadoMatricula.ACTIVA)) {
            Estudiante estudiante = origen.getEstudiante();
            if (estudiante == null || !Boolean.TRUE.equals(estudiante.getActivo())) {
                continue;
            }
            Curso cursoOrigen = origen.getCurso();
            if (cursoOrigen == null) {
                incidencias.add("El estudiante " + estudiante.getId()
                        + " no tiene curso en su matrícula de origen; omitido.");
                continue;
            }
            Integer nivelActual = cursoOrigen.getNivel();
            boolean yaExiste = matriculaRepository.existsByEstudianteIdAndAnio(estudiante.getId(), anioDestino);

            PromocionItemDTO.PromocionItemDTOBuilder item = PromocionItemDTO.builder()
                    .estudianteId(estudiante.getId())
                    .nombreEstudiante(nombreEstudiante(estudiante))
                    .cursoOrigenId(cursoOrigen.getId())
                    .cursoOrigenNombre(cursoOrigen.getNombre())
                    .nivelOrigen(nivelActual)
                    .yaExisteEnDestino(yaExiste);

            if (nivelActual != null && nivelActual >= nivelMax) {
                item.accionPropuesta("EGRESA");
            } else {
                Integer nivelDestino = nivelActual != null ? nivelActual + 1 : null;
                Curso cursoDestino = nivelDestino != null
                        ? cursoRepository.findByNivel(nivelDestino).orElse(null)
                        : null;
                if (cursoDestino != null) {
                    item.accionPropuesta("PROMOVER")
                            .cursoDestinoId(cursoDestino.getId())
                            .cursoDestinoNombre(cursoDestino.getNombre());
                } else {
                    item.accionPropuesta("SIN_CURSO_DESTINO");
                    incidencias.add("No existe un curso de nivel " + nivelDestino
                            + " para promover al estudiante " + estudiante.getId() + ".");
                }
            }
            items.add(item.build());
        }

        return PromocionPreviewResponse.builder()
                .anioOrigen(anioOrigen)
                .anioDestino(anioDestino)
                .items(items)
                .incidencias(incidencias)
                .build();
    }

    /**
     * Aplica la promoción (posiblemente editada). Idempotente (upsert por {@code (estudiante, anio)}),
     * nunca lanza 409 sobre una matrícula destino preexistente. Todo en una transacción.
     */
    @Transactional
    public PromocionResultadoResponse confirmarPromocion(PromocionConfirmRequest req) {
        Integer anioOrigen = req.getAnioOrigen();
        Integer anioDestino = req.getAnioDestino();
        validarAnios(anioOrigen, anioDestino);
        validarTrimestresCerrados(anioOrigen);

        // Deja listos los trimestres CERRADO del año destino.
        trimestreService.crearTrimestresDeAnio(anioDestino);

        int anioActivo = anioEscolarService.resolver(null);
        boolean destinoEsActivo = anioDestino == anioActivo;

        List<String> incidencias = new ArrayList<>();
        int promovidos = 0;
        int repitentes = 0;
        int egresados = 0;
        int excluidos = 0;
        int actualizados = 0;

        List<PromocionDecisionDTO> decisiones = req.getDecisiones() != null
                ? req.getDecisiones()
                : List.of();

        for (PromocionDecisionDTO decision : decisiones) {
            AccionPromocion accion = parsearAccion(decision.getAccion());

            Long estudianteId = decision.getEstudianteId();
            Matricula origen = estudianteId != null
                    ? matriculaRepository.findByEstudianteIdAndAnio(estudianteId, anioOrigen).orElse(null)
                    : null;
            if (origen == null || origen.getEstado() != EstadoMatricula.ACTIVA) {
                incidencias.add("El estudiante " + estudianteId
                        + " no tenía matrícula ACTIVA en " + anioOrigen + "; omitido.");
                continue;
            }
            Estudiante estudiante = origen.getEstudiante();

            switch (accion) {
                case PROMOVER -> {
                    if (decision.getCursoDestinoId() == null) {
                        throw new SchedulingException(
                                "Falta curso destino para promover al estudiante " + estudianteId + ".");
                    }
                    Curso cursoDestino = cursoRepository.findById(decision.getCursoDestinoId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Curso no encontrado con id: " + decision.getCursoDestinoId()));
                    boolean existia = upsertDestino(estudiante, cursoDestino, anioDestino);
                    if (existia) {
                        actualizados++;
                    }
                    promovidos++;
                    if (destinoEsActivo) {
                        actualizarEspejo(estudiante, cursoDestino);
                    }
                }
                case REPITENTE -> {
                    Curso cursoRepite = origen.getCurso();
                    boolean existia = upsertDestino(estudiante, cursoRepite, anioDestino);
                    if (existia) {
                        actualizados++;
                    }
                    repitentes++;
                    if (destinoEsActivo) {
                        actualizarEspejo(estudiante, cursoRepite);
                    }
                }
                case EGRESA -> egresados++;
                case EXCLUIR -> excluidos++;
            }
        }

        return PromocionResultadoResponse.builder()
                .anioOrigen(anioOrigen)
                .anioDestino(anioDestino)
                .promovidos(promovidos)
                .repitentes(repitentes)
                .egresados(egresados)
                .excluidos(excluidos)
                .actualizados(actualizados)
                .incidencias(incidencias)
                .build();
    }

    // Upsert idempotente de la matrícula destino. Devuelve true si actualizó una fila preexistente.
    private boolean upsertDestino(Estudiante estudiante, Curso curso, int anioDestino) {
        String usuario = auditoriaHelper.usuarioActual();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        Matricula destino = matriculaRepository
                .findByEstudianteIdAndAnio(estudiante.getId(), anioDestino)
                .orElse(null);
        if (destino != null) {
            destino.setCurso(curso);
            destino.setEstado(EstadoMatricula.ACTIVA);
            destino.setUsuarioModificacion(usuario);
            destino.setFechaModificacion(hoy);
            destino.setHoraModificacion(ahora);
            matriculaRepository.save(destino);
            return true;
        }
        matriculaRepository.save(Matricula.builder()
                .estudiante(estudiante)
                .curso(curso)
                .anio(anioDestino)
                .estado(EstadoMatricula.ACTIVA)
                .usuarioModificacion(usuario)
                .fechaModificacion(hoy)
                .horaModificacion(ahora)
                .build());
        return false;
    }

    // Sincroniza el espejo (I1): el curso visible en Estudiante cuando el destino es el año activo.
    private void actualizarEspejo(Estudiante estudiante, Curso curso) {
        estudiante.setCurso(curso);
        estudiante.setUsuarioModificacion(auditoriaHelper.usuarioActual());
        estudiante.setFechaModificacion(LocalDate.now());
        estudiante.setHoraModificacion(LocalTime.now());
        estudianteRepository.save(estudiante);
    }

    private void validarAnios(Integer anioOrigen, Integer anioDestino) {
        if (anioOrigen == null || anioDestino == null) {
            throw new SchedulingException("anioOrigen y anioDestino son obligatorios.");
        }
        anioEscolarService.validarRango(anioOrigen);
        anioEscolarService.validarRango(anioDestino);
        if (anioDestino.equals(anioOrigen)) {
            throw new SchedulingException("El año destino debe ser distinto del año origen.");
        }
    }

    // Para promocionar, el año origen debe tener sus tres trimestres creados y todos en estado CERRADO.
    private void validarTrimestresCerrados(Integer anioOrigen) {
        List<Trimestre> trimestres = trimestreRepository.findByAnioOrderByNumeroAsc(anioOrigen);
        boolean todosCerrados = trimestres.size() == 3
                && trimestres.stream().allMatch(t -> t.getEstado() == EstadoTrimestre.CERRADO);
        if (!todosCerrados) {
            throw new SchedulingException("No se puede promocionar: los tres trimestres del año "
                    + anioOrigen + " deben existir y estar cerrados.");
        }
    }

    // Parsea la acción (String) a AccionPromocion; valor nulo/vacío/no mapeable -> 422.
    private AccionPromocion parsearAccion(String accion) {
        if (accion == null || accion.isBlank()) {
            throw new SchedulingException(
                    "Acción de promoción requerida. Valores válidos: PROMOVER, REPITENTE, EXCLUIR, EGRESA");
        }
        try {
            return AccionPromocion.valueOf(accion.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new SchedulingException("Acción de promoción inválida: " + accion
                    + ". Valores válidos: PROMOVER, REPITENTE, EXCLUIR, EGRESA");
        }
    }

    private String nombreEstudiante(Estudiante e) {
        StringBuilder sb = new StringBuilder();
        if (e.getApellidoPaterno() != null && !e.getApellidoPaterno().isBlank()) {
            sb.append(e.getApellidoPaterno().trim());
        }
        if (e.getApellidoMaterno() != null && !e.getApellidoMaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.getApellidoMaterno().trim());
        }
        if (e.getNombre() != null && !e.getNombre().isBlank()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e.getNombre().trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
