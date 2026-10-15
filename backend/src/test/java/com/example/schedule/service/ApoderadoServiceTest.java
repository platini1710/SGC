package com.example.schedule.service;

import com.example.schedule.dto.ApoderadoRequest;
import com.example.schedule.dto.ApoderadoResponse;
import com.example.schedule.exception.DuplicadoException;
import com.example.schedule.model.Apoderado;
import com.example.schedule.model.Estudiante;
import com.example.schedule.repository.ApoderadoRepository;
import com.example.schedule.repository.EstudianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ApoderadoService con repositorios simulados (sin BD):
 *   - create aplica el correo por defecto cuando viene vacío/null.
 *   - create lanza DuplicadoException cuando el RUT ya existe.
 *   - delete desvincula a los estudiantes (apoderado = null) antes de borrar.
 *   - toResponse completa cantidadEstudiantes desde countByApoderadoId.
 */
@ExtendWith(MockitoExtension.class)
class ApoderadoServiceTest {

    private static final String CORREO_DEFAULT = "aespinoza3010@gmail.com";

    @Mock
    private ApoderadoRepository apoderadoRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @InjectMocks
    private ApoderadoService service;

    private ApoderadoRequest request(String rut, String correo) {
        return ApoderadoRequest.builder()
                .rut(rut)
                .nombre("Rosa")
                .apellidoPaterno("Fernández")
                .region("Región Metropolitana")
                .comuna("La Reina")
                .direccion("Los Aromos 123")
                .telefonoCelular("+56912345678")
                .correo(correo)
                .build();
    }

    @Test
    void createAplicaCorreoPorDefectoCuandoVieneVacio() {
        when(apoderadoRepository.existsByRut("9.111.222-3")).thenReturn(false);
        when(apoderadoRepository.save(any(Apoderado.class))).thenAnswer(inv -> {
            Apoderado a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(estudianteRepository.countByApoderadoId(1L)).thenReturn(0);

        ApoderadoResponse resp = service.create(request("9.111.222-3", "  "));

        assertEquals(CORREO_DEFAULT, resp.getCorreo());
    }

    @Test
    void createLanzaDuplicadoCuandoRutExiste() {
        when(apoderadoRepository.existsByRut("9.111.222-3")).thenReturn(true);

        assertThrows(DuplicadoException.class, () -> service.create(request("9.111.222-3", "x@x.cl")));
    }

    @Test
    void deleteDesvinculaEstudiantesAntesDeBorrar() {
        Apoderado apoderado = Apoderado.builder().id(5L).rut("9.111.222-3").build();
        Estudiante e1 = Estudiante.builder().id(1L).apoderado(apoderado).build();
        Estudiante e2 = Estudiante.builder().id(2L).apoderado(apoderado).build();

        when(apoderadoRepository.findById(5L)).thenReturn(java.util.Optional.of(apoderado));
        when(estudianteRepository.findByApoderadoId(5L)).thenReturn(List.of(e1, e2));

        service.delete(5L);

        assertNull(e1.getApoderado());
        assertNull(e2.getApoderado());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Estudiante>> captor = ArgumentCaptor.forClass(List.class);
        verify(estudianteRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        verify(apoderadoRepository).delete(apoderado);
    }

    @Test
    void toResponseCompletaCantidadEstudiantes() {
        Apoderado apoderado = Apoderado.builder()
                .id(7L)
                .rut("9.111.222-3")
                .nombre("Rosa")
                .apellidoPaterno("Fernández")
                .correo(CORREO_DEFAULT)
                .build();
        when(apoderadoRepository.findById(7L)).thenReturn(java.util.Optional.of(apoderado));
        when(estudianteRepository.countByApoderadoId(7L)).thenReturn(3);

        ApoderadoResponse resp = service.findById(7L);

        assertEquals(3, resp.getCantidadEstudiantes());
        assertEquals("Rosa", resp.getNombre());
    }
}
