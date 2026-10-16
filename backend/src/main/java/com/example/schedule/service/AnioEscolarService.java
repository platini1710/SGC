package com.example.schedule.service;

import com.example.schedule.exception.SchedulingException;
import com.example.schedule.model.Parametro;
import com.example.schedule.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class AnioEscolarService {

    private final ParametroRepository parametroRepository;

    /**
     * Resuelve el año escolar efectivo. Si {@code anioOpcional} viene no nulo, se valida
     * su rango y se devuelve. En caso contrario cae al {@code anioEscolarActivo} del
     * parámetro global y, si no hay parámetro, a {@link Year#now()}. Nunca falla al resolver.
     */
    public int resolver(Integer anioOpcional) {
        if (anioOpcional != null) {
            validarRango(anioOpcional);
            return anioOpcional;
        }
        Integer activo = parametroRepository.findTopByOrderByIdAsc()
                .map(Parametro::getAnioEscolarActivo)
                .orElse(null);
        return activo != null ? activo : Year.now().getValue();
    }

    public void validarRango(int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new SchedulingException("Año fuera de rango permitido (2000–2100): " + anio);
        }
    }
}
