package com.example.schedule.service;

import com.example.schedule.dto.TrimestreResponse;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.EstadoTrimestre;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.TrimestreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrimestreService {

    private final TrimestreRepository trimestreRepository;

    public List<TrimestreResponse> findByAnio(Integer anio) {
        int anioResuelto = (anio != null) ? anio : Year.now().getValue();
        return trimestreRepository.findByAnioOrderByNumeroAsc(anioResuelto).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Integer> findDistinctAnios() {
        return trimestreRepository.findDistinctAnios();
    }

    // Crea (idempotentemente) los 3 trimestres del año indicado en estado CERRADO.
    // No abre ningún trimestre: la invariante de "a lo sumo uno ABIERTO" la gestiona abrir().
    @org.springframework.transaction.annotation.Transactional
    public List<TrimestreResponse> crearTrimestresDeAnio(int anio) {
        for (int numero = 1; numero <= 3; numero++) {
            if (trimestreRepository.findByNumeroAndAnio(numero, anio).isEmpty()) {
                trimestreRepository.save(Trimestre.builder()
                        .numero(numero)
                        .anio(anio)
                        .estado(EstadoTrimestre.CERRADO)
                        .build());
            }
        }
        return findByAnio(anio);
    }

    // Solo puede haber un trimestre abierto a la vez en todo el sistema (en cualquier año):
    // al abrir uno, cualquier otro trimestre ABIERTO de cualquier año se cierra automáticamente.
    // Invariante: tras abrir(x), consultar los trimestres ABIERTO devuelve exactamente uno (x).
    @org.springframework.transaction.annotation.Transactional
    public TrimestreResponse abrir(Long id) {
        Trimestre trimestre = getById(id);
        for (Trimestre abierto : trimestreRepository.findByEstado(EstadoTrimestre.ABIERTO)) {
            if (!abierto.getId().equals(id)) {
                abierto.setEstado(EstadoTrimestre.CERRADO);
                trimestreRepository.save(abierto);
            }
        }
        if (trimestre.getEstado() != EstadoTrimestre.ABIERTO) {
            trimestre.setEstado(EstadoTrimestre.ABIERTO);
            trimestre = trimestreRepository.save(trimestre);
        }
        return toResponse(trimestre);
    }

    public TrimestreResponse cerrar(Long id) {
        Trimestre trimestre = getById(id);
        trimestre.setEstado(EstadoTrimestre.CERRADO);
        return toResponse(trimestreRepository.save(trimestre));
    }

    public Trimestre getById(Long id) {
        return trimestreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trimestre no encontrado con id: " + id));
    }

    private TrimestreResponse toResponse(Trimestre t) {
        return TrimestreResponse.builder()
                .id(t.getId())
                .numero(t.getNumero())
                .anio(t.getAnio())
                .estado(t.getEstado().name())
                .build();
    }
}
