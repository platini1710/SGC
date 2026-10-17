package com.example.schedule.controller;

import com.example.schedule.model.Parametro;
import com.example.schedule.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroRepository parametroRepository;

    private static final int TIMEOUT_DEFAULT = 60; // segundos
    private static final String TIPO_JORNADA_DEFAULT = "JEC";
    private static final int DURACION_RECREO_DEFAULT = 15;
    private static final String HORA_INICIO_JORNADA_DEFAULT = "08:00";
    private static final int DURACION_BLOQUE_MIN_DEFAULT = 90;
    private static final int DURACION_ALMUERZO_DEFAULT = 75;

    // Devuelve los parámetros (incluidos los de jornada JEC), null-safe con sus defaults.
    @GetMapping
    public Map<String, Object> getParametros() {
        return parametroRepository.findTopByOrderByIdAsc()
                .map(p -> {
                    int anioEscolarActivo = p.getAnioEscolarActivo() == null
                            ? Year.now().getValue() : p.getAnioEscolarActivo();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota());
                    m.put("timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad());
                    m.put("anioEscolarActivo", anioEscolarActivo);
                    m.put("tipoJornada", p.getTipoJornada() == null ? TIPO_JORNADA_DEFAULT : p.getTipoJornada());
                    m.put("duracionRecreo", p.getDuracionRecreo() == null ? DURACION_RECREO_DEFAULT : p.getDuracionRecreo());
                    m.put("horaInicioJornada", p.getHoraInicioJornada() == null ? HORA_INICIO_JORNADA_DEFAULT : p.getHoraInicioJornada());
                    m.put("duracionBloqueMin", p.getDuracionBloqueMin() == null ? DURACION_BLOQUE_MIN_DEFAULT : p.getDuracionBloqueMin());
                    m.put("duracionAlmuerzo", p.getDuracionAlmuerzo() == null ? DURACION_ALMUERZO_DEFAULT : p.getDuracionAlmuerzo());
                    return m;
                })
                .orElseGet(() -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("aproxNota", "N");
                    m.put("timeoutInactividad", TIMEOUT_DEFAULT);
                    m.put("anioEscolarActivo", Year.now().getValue());
                    m.put("tipoJornada", TIPO_JORNADA_DEFAULT);
                    m.put("duracionRecreo", DURACION_RECREO_DEFAULT);
                    m.put("horaInicioJornada", HORA_INICIO_JORNADA_DEFAULT);
                    m.put("duracionBloqueMin", DURACION_BLOQUE_MIN_DEFAULT);
                    m.put("duracionAlmuerzo", DURACION_ALMUERZO_DEFAULT);
                    return m;
                });
    }

    // Permite cambiar los parámetros con merge parcial (omitir un campo no lo borra).
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Map<String, Object> setParametros(@RequestBody Map<String, Object> body) {
        Parametro p = parametroRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> Parametro.builder().build());

        if (body.containsKey("aproxNota") && body.get("aproxNota") != null) {
            p.setAproxNota(String.valueOf(body.get("aproxNota")));
        }
        if (body.containsKey("timeoutInactividad") && body.get("timeoutInactividad") != null) {
            p.setTimeoutInactividad(Integer.valueOf(String.valueOf(body.get("timeoutInactividad"))));
        }
        if (body.containsKey("anioEscolarActivo") && body.get("anioEscolarActivo") != null) {
            p.setAnioEscolarActivo(Integer.valueOf(String.valueOf(body.get("anioEscolarActivo"))));
        }
        if (body.containsKey("tipoJornada") && body.get("tipoJornada") != null) {
            p.setTipoJornada(String.valueOf(body.get("tipoJornada")));
        }
        if (body.containsKey("duracionRecreo") && body.get("duracionRecreo") != null) {
            p.setDuracionRecreo(Integer.valueOf(String.valueOf(body.get("duracionRecreo"))));
        }
        if (body.containsKey("horaInicioJornada") && body.get("horaInicioJornada") != null) {
            p.setHoraInicioJornada(String.valueOf(body.get("horaInicioJornada")));
        }
        if (body.containsKey("duracionBloqueMin") && body.get("duracionBloqueMin") != null) {
            p.setDuracionBloqueMin(Integer.valueOf(String.valueOf(body.get("duracionBloqueMin"))));
        }
        if (body.containsKey("duracionAlmuerzo") && body.get("duracionAlmuerzo") != null) {
            p.setDuracionAlmuerzo(Integer.valueOf(String.valueOf(body.get("duracionAlmuerzo"))));
        }
        parametroRepository.save(p);

        int anioEscolarActivo = p.getAnioEscolarActivo() == null
                ? Year.now().getValue() : p.getAnioEscolarActivo();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("aproxNota", p.getAproxNota() == null ? "N" : p.getAproxNota());
        m.put("timeoutInactividad", p.getTimeoutInactividad() == null ? TIMEOUT_DEFAULT : p.getTimeoutInactividad());
        m.put("anioEscolarActivo", anioEscolarActivo);
        m.put("tipoJornada", p.getTipoJornada() == null ? TIPO_JORNADA_DEFAULT : p.getTipoJornada());
        m.put("duracionRecreo", p.getDuracionRecreo() == null ? DURACION_RECREO_DEFAULT : p.getDuracionRecreo());
        m.put("horaInicioJornada", p.getHoraInicioJornada() == null ? HORA_INICIO_JORNADA_DEFAULT : p.getHoraInicioJornada());
        m.put("duracionBloqueMin", p.getDuracionBloqueMin() == null ? DURACION_BLOQUE_MIN_DEFAULT : p.getDuracionBloqueMin());
        m.put("duracionAlmuerzo", p.getDuracionAlmuerzo() == null ? DURACION_ALMUERZO_DEFAULT : p.getDuracionAlmuerzo());
        return m;
    }
}
