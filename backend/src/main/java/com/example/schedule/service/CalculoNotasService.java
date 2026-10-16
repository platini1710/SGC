package com.example.schedule.service;

import com.example.schedule.model.Nota;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.NotaRepository;
import com.example.schedule.repository.TrimestreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalculoNotasService {

    private static final BigDecimal CUATRO = new BigDecimal("4.00");
    private static final BigDecimal UMBRAL = new BigDecimal("3.95");

    private final NotaRepository notaRepository;
    private final TrimestreRepository trimestreRepository;
    private final com.example.schedule.repository.ParametroRepository parametroRepository;

    private BigDecimal redondear2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // Devuelve true si el parámetro aproxNota está en 'S' (ignora mayúsculas/espacios).
    private boolean debeAproximar() {
        return parametroRepository.findTopByOrderByIdAsc()
                .map(p -> "S".equalsIgnoreCase(p.getAproxNota() == null ? "" : p.getAproxNota().trim()))
                .orElse(false);
    }

    // Lleva un promedio (2 decimales) a 1 decimal según el parámetro:
    //  - aproxNota = 'S'  -> redondea al decimal superior (HALF_UP): 6.35 -> 6.4
    //  - en otro caso     -> trunca el segundo decimal (DOWN):       6.35 -> 6.3
    private BigDecimal aplicarAproximacion(BigDecimal promedio2dec) {
        if (promedio2dec == null) {
            return null;
        }
        RoundingMode modo = debeAproximar() ? RoundingMode.HALF_UP : RoundingMode.DOWN;
        return promedio2dec.setScale(1, modo);
    }

    public BigDecimal promedioTrimestre(Long estudianteId, Long asignaturaId, Long trimestreId) {
        List<Nota> notas = notaRepository
                .findByEstudianteIdAndAsignaturaIdAndTrimestreId(estudianteId, asignaturaId, trimestreId);
        if (notas.isEmpty()) {
            return null;
        }
        BigDecimal suma = notas.stream()
                .map(Nota::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(new BigDecimal(notas.size()), 2, RoundingMode.HALF_UP);
        return aplicarAproximacion(redondear2(promedio));
    }

    public BigDecimal notaFinalAnual(Long estudianteId, Long asignaturaId, int anio) {
        // La nota final anual SOLO se calcula cuando los tres trimestres del año
        // existen y cada uno tiene al menos una nota para el estudiante/asignatura.
        // Si falta cualquiera de los tres promedios, la NF anual queda vacía (null).
        List<BigDecimal> presentes = new ArrayList<>();
        for (int numero = 1; numero <= 3; numero++) {
            Optional<Trimestre> trimestre = trimestreRepository.findByNumeroAndAnio(numero, anio);
            if (trimestre.isEmpty()) {
                return null;
            }
            BigDecimal promedio = promedioTrimestre(estudianteId, asignaturaId, trimestre.get().getId());
            if (promedio == null) {
                return null;
            }
            presentes.add(promedio);
        }
        if (presentes.size() < 3) {
            return null;
        }
        BigDecimal suma = presentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nf = suma.divide(new BigDecimal(presentes.size()), 2, RoundingMode.HALF_UP);
        if (nf.compareTo(UMBRAL) >= 0 && nf.compareTo(CUATRO) < 0) {
            nf = CUATRO;
        }
        // Aplica la aproximación a un decimal según el parámetro aproxNota.
        return aplicarAproximacion(nf);
    }
}
