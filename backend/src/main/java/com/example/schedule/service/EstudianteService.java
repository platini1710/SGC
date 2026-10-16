package com.example.schedule.service;

import com.example.schedule.dto.EstudianteRequest;
import com.example.schedule.dto.EstudianteResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Apoderado;
import com.example.schedule.model.Curso;
import com.example.schedule.model.EstadoMatricula;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Matricula;
import com.example.schedule.repository.ApoderadoRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
import com.example.schedule.security.AuditoriaHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnioEscolarService anioEscolarService;
    private final AuditoriaHelper auditoriaHelper;

    public Page<EstudianteResponse> findAll(Pageable pageable) {
        // Solo estudiantes activos (soft delete).
        return estudianteRepository.findByActivoTrueOrderByApellidoPaternoAsc(pageable).map(this::toResponse);
    }

    public Page<EstudianteResponse> findByCurso(Long cursoId, Integer anio, Pageable pageable) {
        // Alumnos activos del curso según su matrícula del año (default = año escolar activo).
        // El orden lo fija la @Query del repositorio; se descarta el Sort entrante.
        int anioResuelto = anioEscolarService.resolver(anio);
        return matriculaRepository
                .findEstudiantesByCursoAndAnio(cursoId, anioResuelto,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(this::toResponse);
    }

    public EstudianteResponse findById(Long id) {
        return toResponse(getById(id));
    }

    public EstudianteResponse create(EstudianteRequest request) {
        Curso curso = null;
        if (request.getCursoId() != null) {
            curso = cursoRepository.findById(request.getCursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + request.getCursoId()));
        }
        Apoderado apoderado = resolverApoderado(request.getApoderadoId());
        Estudiante estudiante = Estudiante.builder()
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .rut(request.getRut())
                .fechaNacimiento(request.getFechaNacimiento())
                .curso(curso)
                .apoderado(apoderado)
                .lugarNacimiento(request.getLugarNacimiento())
                .nacionalidad(request.getNacionalidad())
                .sexo(request.getSexo())
                .comuna(request.getComuna())
                .telefono(request.getTelefono())
                .domicilio(request.getDomicilio())
                .foto(request.getFoto())
                .padreNombre(request.getPadreNombre())
                .padreRut(request.getPadreRut())
                .padreOcupacion(request.getPadreOcupacion())
                .madreNombre(request.getMadreNombre())
                .madreRut(request.getMadreRut())
                .madreOcupacion(request.getMadreOcupacion())
                .apoderadoRelacion(request.getApoderadoRelacion())
                .prevision(request.getPrevision())
                .alergias(request.getAlergias())
                .condicionMedica(request.getCondicionMedica())
                .contactoEmergencia(request.getContactoEmergencia())
                .fonoEmergencia(request.getFonoEmergencia())
                .usuarioModificacion(auditoriaHelper.usuarioActual())
                .fechaModificacion(LocalDate.now())
                .horaModificacion(LocalTime.now())
                .build();
        Estudiante guardado = estudianteRepository.save(estudiante);
        if (request.getCursoId() != null) {
            sincronizarMatriculaAnioActivo(guardado, curso);
        }
        return toResponse(guardado);
    }

    public EstudianteResponse update(Long id, EstudianteRequest request) {
        Estudiante estudiante = getById(id);
        estudiante.setNombre(request.getNombre());
        estudiante.setApellidoPaterno(request.getApellidoPaterno());
        estudiante.setApellidoMaterno(request.getApellidoMaterno());
        // RUT inmutable: no se modifica en update (solo se fija al crear).
        estudiante.setFechaNacimiento(request.getFechaNacimiento());
        Curso curso = null;
        if (request.getCursoId() != null) {
            curso = cursoRepository.findById(request.getCursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + request.getCursoId()));
            estudiante.setCurso(curso);
        }
        estudiante.setApoderado(resolverApoderado(request.getApoderadoId()));
        estudiante.setLugarNacimiento(request.getLugarNacimiento());
        estudiante.setNacionalidad(request.getNacionalidad());
        estudiante.setSexo(request.getSexo());
        estudiante.setComuna(request.getComuna());
        estudiante.setTelefono(request.getTelefono());
        estudiante.setDomicilio(request.getDomicilio());
        estudiante.setFoto(request.getFoto());
        estudiante.setPadreNombre(request.getPadreNombre());
        estudiante.setPadreRut(request.getPadreRut());
        estudiante.setPadreOcupacion(request.getPadreOcupacion());
        estudiante.setMadreNombre(request.getMadreNombre());
        estudiante.setMadreRut(request.getMadreRut());
        estudiante.setMadreOcupacion(request.getMadreOcupacion());
        estudiante.setApoderadoRelacion(request.getApoderadoRelacion());
        estudiante.setPrevision(request.getPrevision());
        estudiante.setAlergias(request.getAlergias());
        estudiante.setCondicionMedica(request.getCondicionMedica());
        estudiante.setContactoEmergencia(request.getContactoEmergencia());
        estudiante.setFonoEmergencia(request.getFonoEmergencia());
        estudiante.setUsuarioModificacion(auditoriaHelper.usuarioActual());
        estudiante.setFechaModificacion(LocalDate.now());
        estudiante.setHoraModificacion(LocalTime.now());
        Estudiante guardado = estudianteRepository.save(estudiante);
        if (request.getCursoId() != null) {
            sincronizarMatriculaAnioActivo(guardado, curso);
        }
        return toResponse(guardado);
    }

    // Upsert (I1) de la matrícula del año escolar activo: si existe actualiza el curso,
    // si no existe la crea ACTIVA. Nunca lanza DuplicadoException/409.
    private void sincronizarMatriculaAnioActivo(Estudiante estudiante, Curso curso) {
        int anioActivo = anioEscolarService.resolver(null);
        String usuario = auditoriaHelper.usuarioActual();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        Matricula matricula = matriculaRepository
                .findByEstudianteIdAndAnio(estudiante.getId(), anioActivo)
                .orElse(null);
        if (matricula != null) {
            matricula.setCurso(curso);
            matricula.setUsuarioModificacion(usuario);
            matricula.setFechaModificacion(hoy);
            matricula.setHoraModificacion(ahora);
            matriculaRepository.save(matricula);
        } else {
            matriculaRepository.save(Matricula.builder()
                    .estudiante(estudiante)
                    .curso(curso)
                    .anio(anioActivo)
                    .estado(EstadoMatricula.ACTIVA)
                    .usuarioModificacion(usuario)
                    .fechaModificacion(hoy)
                    .horaModificacion(ahora)
                    .build());
        }
    }

    // Soft delete: marca al estudiante como inactivo en vez de borrarlo.
    public void delete(Long id) {
        Estudiante estudiante = getById(id);
        estudiante.setActivo(false);
        estudiante.setInactivadoPor(auditoriaHelper.usuarioActual());
        estudiante.setFechaInactivacion(LocalDate.now());
        estudiante.setHoraInactivacion(LocalTime.now());
        estudianteRepository.save(estudiante);
    }

    // Reactiva un estudiante inactivo: lo vuelve activo y limpia la auditoría de inactivación.
    public EstudianteResponse reactivar(Long id) {
        Estudiante estudiante = getById(id);
        estudiante.setActivo(true);
        estudiante.setInactivadoPor(null);
        estudiante.setFechaInactivacion(null);
        estudiante.setHoraInactivacion(null);
        estudiante.setUsuarioModificacion(auditoriaHelper.usuarioActual());
        estudiante.setFechaModificacion(LocalDate.now());
        estudiante.setHoraModificacion(LocalTime.now());
        return toResponse(estudianteRepository.save(estudiante));
    }

    public Page<EstudianteResponse> findInactivos(Pageable pageable) {
        return estudianteRepository.findByActivoFalseOrderByApellidoPaternoAsc(pageable).map(this::toResponse);
    }

    private Estudiante getById(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + id));
    }

    private Apoderado resolverApoderado(Long apoderadoId) {
        if (apoderadoId == null) {
            return null;
        }
        return apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con id: " + apoderadoId));
    }

    private String unirApellidos(String paterno, String materno) {
        StringBuilder sb = new StringBuilder();
        if (paterno != null && !paterno.isBlank()) {
            sb.append(paterno.trim());
        }
        if (materno != null && !materno.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(materno.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private EstudianteResponse toResponse(Estudiante e) {
        return EstudianteResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellidoPaterno(e.getApellidoPaterno())
                .apellidoMaterno(e.getApellidoMaterno())
                .apellido(unirApellidos(e.getApellidoPaterno(), e.getApellidoMaterno()))
                .rut(e.getRut())
                .fechaNacimiento(e.getFechaNacimiento())
                .cursoId(e.getCurso() != null ? e.getCurso().getId() : null)
                .nombreCurso(e.getCurso() != null ? e.getCurso().getNombre() : null)
                .apoderadoId(e.getApoderado() != null ? e.getApoderado().getId() : null)
                .nombreApoderado(e.getApoderado() != null
                        ? e.getApoderado().getNombre() + " " + e.getApoderado().getApellidoPaterno()
                        : null)
                .lugarNacimiento(e.getLugarNacimiento())
                .nacionalidad(e.getNacionalidad())
                .sexo(e.getSexo())
                .comuna(e.getComuna())
                .telefono(e.getTelefono())
                .domicilio(e.getDomicilio())
                .foto(e.getFoto())
                .padreNombre(e.getPadreNombre())
                .padreRut(e.getPadreRut())
                .padreOcupacion(e.getPadreOcupacion())
                .madreNombre(e.getMadreNombre())
                .madreRut(e.getMadreRut())
                .madreOcupacion(e.getMadreOcupacion())
                .apoderadoRelacion(e.getApoderadoRelacion())
                .prevision(e.getPrevision())
                .alergias(e.getAlergias())
                .condicionMedica(e.getCondicionMedica())
                .contactoEmergencia(e.getContactoEmergencia())
                .fonoEmergencia(e.getFonoEmergencia())
                .activo(e.getActivo())
                .usuarioModificacion(e.getUsuarioModificacion())
                .fechaModificacion(e.getFechaModificacion())
                .horaModificacion(e.getHoraModificacion())
                .inactivadoPor(e.getInactivadoPor())
                .fechaInactivacion(e.getFechaInactivacion())
                .horaInactivacion(e.getHoraInactivacion())
                .build();
    }
}
