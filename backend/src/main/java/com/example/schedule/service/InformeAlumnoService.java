package com.example.schedule.service;

import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Colegio;
import com.example.schedule.model.Curso;
import com.example.schedule.model.CursoAsignatura;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Nota;
import com.example.schedule.model.Profesor;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.NotaRepository;
import com.example.schedule.repository.TrimestreRepository;
import com.example.schedule.security.NotaAccessChecker;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el "Informe de Avance de Notas Parciales" (boletín POR ALUMNO).
 * Documento vertical A4; una página por alumno cuando no se indica estudiante.
 * Es un reporte NUEVO y aislado del reporte por asignatura ({@link ReporteService}).
 */
@Service
@RequiredArgsConstructor
public class InformeAlumnoService {

    private static final int MAX_NOTAS = 9;
    private static final String SIN_DATO = "-";
    private static final BigDecimal UMBRAL_ROJO = new BigDecimal("4.0");

    private final CursoRepository cursoRepository;
    private final TrimestreRepository trimestreRepository;
    private final EstudianteRepository estudianteRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final NotaRepository notaRepository;
    private final CalculoNotasService calculoNotasService;
    private final ColegioRepository colegioRepository;
    private final NotaAccessChecker notaAccessChecker;

    public byte[] generarBoletin(Long cursoId, Long trimestreId, Long estudianteId) {
        notaAccessChecker.verificarCurso(cursoId);

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + cursoId));
        Trimestre trimestre = trimestreRepository.findById(trimestreId)
                .orElseThrow(() -> new ResourceNotFoundException("Trimestre no encontrado con id: " + trimestreId));

        List<Estudiante> alumnos;
        if (estudianteId != null) {
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estudiante no encontrado con id: " + estudianteId));
            if (estudiante.getCurso() == null || !cursoId.equals(estudiante.getCurso().getId())) {
                throw new ResourceNotFoundException(
                        "El estudiante " + estudianteId + " no pertenece al curso " + cursoId);
            }
            alumnos = List.of(estudiante);
        } else {
            alumnos = estudianteRepository.findByCursoIdOrderByApellidoAscNombreAsc(cursoId);
        }

        List<CursoAsignatura> asignaturas = new ArrayList<>(
                cursoAsignaturaRepository.findByCursoId(cursoId));
        asignaturas.sort(Comparator.comparing(
                ca -> ca.getAsignatura() != null && ca.getAsignatura().getNombre() != null
                        ? ca.getAsignatura().getNombre()
                        : "",
                String.CASE_INSENSITIVE_ORDER));

        Colegio colegio = colegioRepository.findTopByOrderByIdAsc().orElse(null);

        Document document = new Document(PageSize.A4, 36, 36, 36, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        try {
            if (alumnos.isEmpty()) {
                // Curso sin estudiantes: emitir un documento válido (encabezado + aviso).
                escribirEncabezado(document, colegio, trimestre);
                Paragraph vacio = new Paragraph("El curso no tiene estudiantes.",
                        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
                vacio.setSpacingBefore(12f);
                document.add(vacio);
            } else {
                boolean primero = true;
                for (Estudiante alumno : alumnos) {
                    if (!primero) {
                        document.newPage();
                    }
                    primero = false;
                    escribirEncabezado(document, colegio, trimestre);
                    escribirAlumno(document, colegio, curso, trimestre, alumno, asignaturas);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el informe PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void escribirEncabezado(Document document, Colegio colegio, Trimestre trimestre) throws Exception {
        if (colegio != null) {
            try {
                PdfPTable cabecera = new PdfPTable(2);
                cabecera.setWidthPercentage(100);
                cabecera.setWidths(new float[]{5f, 1.3f});

                PdfPCell texto = new PdfPCell();
                texto.setBorder(0);
                texto.setVerticalAlignment(Element.ALIGN_MIDDLE);
                if (colegio.getNombre() != null) {
                    Paragraph nombre = new Paragraph(colegio.getNombre(),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
                    texto.addElement(nombre);
                }
                String detalle = unir(colegio.getDireccion(), colegio.getComuna(), colegio.getRegion());
                if (!detalle.isEmpty()) {
                    Paragraph p = new Paragraph(detalle,
                            FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
                    texto.addElement(p);
                }
                cabecera.addCell(texto);

                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(0);
                logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                Image logo = cargarLogo(colegio.getLogo());
                if (logo != null) {
                    logo.scaleToFit(70, 70);
                    logoCell.addElement(logo);
                }
                cabecera.addCell(logoCell);

                document.add(cabecera);
            } catch (Exception e) {
                if (colegio.getNombre() != null) {
                    Paragraph nombre = new Paragraph(colegio.getNombre(),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
                    nombre.setAlignment(Element.ALIGN_CENTER);
                    document.add(nombre);
                }
            }
        }

        String periodo = trimestre.getNumero() != null ? "TRIMESTRE " + trimestre.getNumero() : "TRIMESTRE";
        Paragraph titulo = new Paragraph("INFORME AVANCE DE NOTAS PARCIALES  \u00b7  " + periodo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(10f);
        document.add(titulo);

        if (trimestre.getAnio() != null) {
            Paragraph anio = new Paragraph("A\u00f1o " + trimestre.getAnio(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10));
            anio.setAlignment(Element.ALIGN_CENTER);
            anio.setSpacingAfter(10f);
            document.add(anio);
        }
    }

    private void escribirAlumno(Document document, Colegio colegio, Curso curso, Trimestre trimestre,
                                Estudiante estudiante, List<CursoAsignatura> asignaturas) throws Exception {
        // Bloque de datos (etiqueta | valor).
        PdfPTable datos = new PdfPTable(2);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[]{1.3f, 4f});
        datos.setSpacingBefore(6f);
        datos.setSpacingAfter(10f);

        datos.addCell(celdaEtiqueta("Alumno:"));
        datos.addCell(celdaDato(nombreCompletoEstudiante(estudiante)));
        datos.addCell(celdaEtiqueta("Curso:"));
        datos.addCell(celdaDato(curso.getNombre() != null ? curso.getNombre() : SIN_DATO));
        datos.addCell(celdaEtiqueta("Profesor(a) Jefe:"));
        datos.addCell(celdaDato(nombreProfesorJefe(curso)));
        document.add(datos);

        // Tabla principal: Asignatura + N1..N9 + Promedio = 11 columnas.
        PdfPTable tabla = new PdfPTable(MAX_NOTAS + 2);
        tabla.setWidthPercentage(100);
        float[] widths = new float[MAX_NOTAS + 2];
        widths[0] = 4f;
        for (int i = 1; i <= MAX_NOTAS; i++) {
            widths[i] = 1f;
        }
        widths[MAX_NOTAS + 1] = 1.5f;
        tabla.setWidths(widths);

        // Fila de cabecera agrupada.
        PdfPCell hAsignaturas = celdaHeader("Asignaturas", Element.ALIGN_LEFT);
        hAsignaturas.setRowspan(2);
        hAsignaturas.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(hAsignaturas);

        PdfPCell hCalificaciones = celdaHeader("Calificaciones", Element.ALIGN_CENTER);
        hCalificaciones.setColspan(MAX_NOTAS);
        tabla.addCell(hCalificaciones);

        PdfPCell hPromedio = celdaHeader("Promedio", Element.ALIGN_CENTER);
        hPromedio.setRowspan(2);
        hPromedio.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(hPromedio);

        for (int i = 1; i <= MAX_NOTAS; i++) {
            tabla.addCell(celdaHeader("N" + i, Element.ALIGN_CENTER));
        }
        tabla.setHeaderRows(2);

        // Una fila por asignatura.
        List<BigDecimal> promedios = new ArrayList<>();
        for (CursoAsignatura ca : asignaturas) {
            if (ca.getAsignatura() == null) {
                continue;
            }
            Long asignaturaId = ca.getAsignatura().getId();
            String nombre = ca.getAsignatura().getNombre() != null ? ca.getAsignatura().getNombre() : SIN_DATO;
            tabla.addCell(celdaTexto(nombre, Element.ALIGN_LEFT, false));

            Map<Integer, BigDecimal> porPosicion = mapearNotas(
                    notaRepository.findByEstudianteIdAndAsignaturaIdAndTrimestreId(
                            estudiante.getId(), asignaturaId, trimestre.getId()));
            for (int pos = 1; pos <= MAX_NOTAS; pos++) {
                tabla.addCell(celdaValor(porPosicion.get(pos)));
            }

            BigDecimal promedio = calculoNotasService.promedioTrimestre(
                    estudiante.getId(), asignaturaId, trimestre.getId());
            if (promedio != null) {
                promedios.add(promedio);
            }
            tabla.addCell(celdaValor(promedio));
        }

        // Fila final "Promedio General".
        PdfPCell etiquetaGeneral = celdaTexto("Promedio General", Element.ALIGN_RIGHT, true);
        etiquetaGeneral.setColspan(MAX_NOTAS + 1);
        etiquetaGeneral.setBackgroundColor(new Color(230, 234, 242));
        etiquetaGeneral.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(etiquetaGeneral);
        BigDecimal general = promedioGeneral(promedios);
        PdfPCell celdaGeneral = general != null
                ? celdaValor(general)
                : celdaTexto(SIN_DATO, Element.ALIGN_CENTER, false);
        celdaGeneral.setBackgroundColor(new Color(230, 234, 242));
        tabla.addCell(celdaGeneral);

        document.add(tabla);

        // Pie: observaciones, firma, lugar y fecha.
        Paragraph observaciones = new Paragraph("Observaciones:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
        observaciones.setSpacingBefore(18f);
        document.add(observaciones);

        Paragraph lineasObs = new Paragraph("\n\n\n",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(lineasObs);

        Paragraph firmaLinea = new Paragraph("____________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        firmaLinea.setAlignment(Element.ALIGN_CENTER);
        firmaLinea.setSpacingBefore(24f);
        document.add(firmaLinea);

        Paragraph firma = new Paragraph("Profesor(a) Jefe",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        firma.setAlignment(Element.ALIGN_CENTER);
        document.add(firma);

        String comuna = colegio != null && colegio.getComuna() != null ? colegio.getComuna() : "____________";
        String anio = trimestre.getAnio() != null ? String.valueOf(trimestre.getAnio()) : "______";
        Paragraph lugarFecha = new Paragraph(
                comuna + ", ____ de ____________ de " + anio,
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        lugarFecha.setAlignment(Element.ALIGN_CENTER);
        lugarFecha.setSpacingBefore(18f);
        document.add(lugarFecha);
    }

    /**
     * Mapea las notas por posición (1..9). Posiciones ausentes no aparecen y
     * quien consume rinde celda vacía.
     */
    Map<Integer, BigDecimal> mapearNotas(List<Nota> notas) {
        Map<Integer, BigDecimal> porPosicion = new HashMap<>();
        if (notas == null) {
            return porPosicion;
        }
        for (Nota nota : notas) {
            if (nota.getPosicion() != null) {
                porPosicion.put(nota.getPosicion(), nota.getValor());
            }
        }
        return porPosicion;
    }

    /**
     * Promedio simple de los promedios de asignatura no nulos, a 1 decimal
     * {@link RoundingMode#HALF_UP}. Devuelve null si no hay ninguno.
     */
    BigDecimal promedioGeneral(List<BigDecimal> promedios) {
        if (promedios == null || promedios.isEmpty()) {
            return null;
        }
        BigDecimal suma = promedios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(new BigDecimal(promedios.size()), 1, RoundingMode.HALF_UP);
    }

    private String nombreCompletoEstudiante(Estudiante estudiante) {
        return unirNombre(estudiante.getNombre(), estudiante.getApellido());
    }

    private String nombreProfesorJefe(Curso curso) {
        Profesor jefe = curso.getProfesorJefe();
        if (jefe == null) {
            return SIN_DATO;
        }
        String nombre = unirNombre(jefe.getNombre(), jefe.getApellido());
        return nombre.isEmpty() ? SIN_DATO : nombre;
    }

    private String unirNombre(String nombre, String apellido) {
        StringBuilder sb = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) {
            sb.append(nombre.trim());
        }
        if (apellido != null && !apellido.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(apellido.trim());
        }
        return sb.toString();
    }

    /**
     * Carga el logo (data URI Base64) como Image, o null si no es incrustable
     * (SVG, Base64 inválido, formato no soportado). Nunca lanza.
     */
    private Image cargarLogo(String logo) {
        try {
            if (logo == null || !logo.startsWith("data:image") || logo.contains("svg")) {
                return null;
            }
            int coma = logo.indexOf(',');
            if (coma < 0) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(logo.substring(coma + 1).trim());
            return Image.getInstance(bytes);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(" \u00b7 ");
                }
                sb.append(parte);
            }
        }
        return sb.toString();
    }

    private PdfPCell celdaHeader(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(4f);
        cell.setBackgroundColor(new Color(55, 71, 100));
        return cell;
    }

    private PdfPCell celdaTexto(String texto, int alineacion, boolean negrita) {
        Font font = negrita
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)
                : FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell celdaValor(BigDecimal valor) {
        String texto = valor != null ? valor.toPlainString() : "";
        boolean rojo = valor != null && valor.compareTo(UMBRAL_ROJO) < 0;
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, rojo ? Color.RED : Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell celdaEtiqueta(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setBorder(0);
        cell.setPadding(3f);
        return cell;
    }

    private PdfPCell celdaDato(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBorder(0);
        cell.setPadding(3f);
        return cell;
    }
}
