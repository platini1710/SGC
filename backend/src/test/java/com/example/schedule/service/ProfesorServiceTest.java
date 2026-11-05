package com.example.schedule.service;

import com.example.schedule.dto.ProfesorRequest;
import com.example.schedule.exception.DuplicadoException;
import com.example.schedule.model.Profesor;
import com.example.schedule.repository.EspecialidadRepository;
import com.example.schedule.repository.HorarioRepository;
import com.example.schedule.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfesorServiceTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private EspecialidadRepository especialidadRepository;
    @Mock
    private HorarioRepository horarioRepository;

    @InjectMocks
    private ProfesorService service;

    private ProfesorRequest req(String email) {
        return ProfesorRequest.builder().nombre("Ana").apellido("P").email(email)
                .rut("1-9").especialidadIds(List.of()).build();
    }

    private Profesor profesor(Long id, String email) {
        return Profesor.builder().id(id).nombre("Ana").apellido("P").email(email).rut("1-9").activo(true).build();
    }

    @Test
    void create_emailDeOtroProfesor_lanzaDuplicado() {
        when(profesorRepository.findByEmail("a@b.cl")).thenReturn(Optional.of(profesor(5L, "a@b.cl")));

        DuplicadoException ex = assertThrows(DuplicadoException.class, () -> service.create(req("a@b.cl")));
        assertEquals("El correo electrónico ya está registrado en otro profesor.", ex.getMessage());
        verify(profesorRepository, never()).save(any());
    }

    @Test
    void create_emailLibre_guarda() {
        when(profesorRepository.findByEmail("a@b.cl")).thenReturn(Optional.empty());
        when(especialidadRepository.findAllById(any())).thenReturn(List.of());
        when(profesorRepository.save(any())).thenAnswer(i -> {
            Profesor p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        service.create(req("a@b.cl"));

        verify(profesorRepository).save(any(Profesor.class));
    }

    @Test
    void update_mismoProfesorConservaSuEmail_noLanza() {
        when(profesorRepository.findById(5L)).thenReturn(Optional.of(profesor(5L, "a@b.cl")));
        when(profesorRepository.findByEmail("a@b.cl")).thenReturn(Optional.of(profesor(5L, "a@b.cl")));
        when(especialidadRepository.findAllById(any())).thenReturn(List.of());
        when(profesorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update(5L, req("a@b.cl"));

        verify(profesorRepository).save(any(Profesor.class));
    }

    @Test
    void update_emailDeOtroProfesor_lanzaDuplicado() {
        when(profesorRepository.findById(5L)).thenReturn(Optional.of(profesor(5L, "viejo@b.cl")));
        when(profesorRepository.findByEmail("a@b.cl")).thenReturn(Optional.of(profesor(9L, "a@b.cl")));

        assertThrows(DuplicadoException.class, () -> service.update(5L, req("a@b.cl")));
        verify(profesorRepository, never()).save(any());
    }

    @Test
    void create_emailVacio_noConsultaUnicidad() {
        lenient().when(especialidadRepository.findAllById(any())).thenReturn(List.of());
        when(profesorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(req(""));

        verify(profesorRepository, never()).findByEmail(any());
    }
}
