package com.example.schedule.service;

import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Colegio;
import com.example.schedule.model.Curso;
import com.example.schedule.model.CursoAsignatura;
import com.example.schedule.model.Estudiante;
import com.example.schedule.model.Profesor;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoAsignaturaRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.EstudianteRepository;
import com.example.schedule.repository.MatriculaRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Genera el "Certificado Anual de Estudios" (una página por alumno).
 * Documento vertical A4. Es un reporte NUEVO y aislado de los reportes por
 * asignatura ({@link ReporteService}) y del boletín por alumno
 * ({@link InformeAlumnoService}); replica el patrón de estos (cargarLogo,
 * helpers de celda, nombreCompleto) sin reutilizar sus métodos privados.
 */
@Service
@RequiredArgsConstructor
public class CertificadoService {

    private static final String SIN_DATO = "-";
    private static final BigDecimal UMBRAL_APROBACION = new BigDecimal("4.0");
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] UNIDADES =
            {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"};

    private final CursoRepository cursoRepository;
    private final EstudianteRepository estudianteRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final ColegioRepository colegioRepository;
    private final CalculoNotasService calculoNotasService;
    private final NotaAccessChecker notaAccessChecker;
    private final MatriculaRepository matriculaRepository;
    private final AnioEscolarService anioEscolarService;

    public byte[] generarCertificado(Long cursoId, Integer anio, Long estudianteId) {
        notaAccessChecker.verificarCurso(cursoId);

        int anioEfectivo = anioEscolarService.resolver(anio);

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id: " + cursoId));

        Colegio colegio = colegioRepository.findTopByOrderByIdAsc().orElse(null);

        List<CursoAsignatura> asignaturas = new ArrayList<>(
                cursoAsignaturaRepository.findByCursoId(cursoId));
        asignaturas.sort(Comparator.comparing(
                ca -> ca.getAsignatura() != null && ca.getAsignatura().getNombre() != null
                        ? ca.getAsignatura().getNombre()
                        : "",
                String.CASE_INSENSITIVE_ORDER));

        List<Estudiante> alumnos;
        if (estudianteId != null) {
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estudiante no encontrado con id: " + estudianteId));
            Long cursoDelAlumno = matriculaRepository.findByEstudianteIdAndAnio(estudianteId, anioEfectivo)
                    .map(m -> m.getCurso() != null ? m.getCurso().getId() : null)
                    .orElseGet(() -> estudiante.getCurso() != null ? estudiante.getCurso().getId() : null);
            if (cursoDelAlumno == null || !cursoId.equals(cursoDelAlumno)) {
                throw new ResourceNotFoundException(
                        "El estudiante " + estudianteId + " no pertenece al curso " + cursoId
                                + " en el a\u00f1o " + anioEfectivo);
            }
            alumnos = List.of(estudiante);
        } else {
            alumnos = matriculaRepository.findEstudiantesByCursoAndAnio(cursoId, anioEfectivo);
        }

        Document document = new Document(PageSize.A4, 40, 40, 40, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        try {
            if (alumnos.isEmpty()) {
                escribirCabecera(document, colegio, anioEfectivo);
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
                    escribirCertificado(document, colegio, curso, alumno, asignaturas, anioEfectivo);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el certificado PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void escribirCabecera(Document document, Colegio colegio, int anio) throws Exception {
        PdfPTable cabecera = new PdfPTable(2);
        cabecera.setWidthPercentage(100);
        cabecera.setWidths(new float[]{3.2f, 2f});

        // Columna izquierda: encabezado institucional + logo.
        PdfPCell izq = new PdfPCell();
        izq.setBorder(0);
        Image logo = cargarLogo(colegio != null ? colegio.getLogo() : null);
        if (logo != null) {
            logo.scaleToFit(60, 60);
            Paragraph logoPar = new Paragraph();
            logoPar.add(new com.lowagie.text.Chunk(logo, 0, 0));
            izq.addElement(logoPar);
        }
        izq.addElement(lineaIzq("REPÚBLICA DE CHILE", true));
        izq.addElement(lineaIzq("MINISTERIO DE EDUCACIÓN", false));
        izq.addElement(lineaIzq("DIVISIÓN DE EDUCACIÓN GENERAL", false));
        izq.addElement(lineaIzq("SECRETARÍA REGIONAL MINISTERIAL DE EDUCACIÓN", false));
        cabecera.addCell(izq);

        // Columna derecha: datos regionales del colegio.
        PdfPCell der = new PdfPCell();
        der.setBorder(0);
        der.setHorizontalAlignment(Element.ALIGN_RIGHT);
        der.addElement(lineaDer("Región: " + valor(colegio != null ? colegio.getRegion() : null)));
        der.addElement(lineaDer("Provincia: " + valor(colegio != null ? colegio.getProvincia() : null)));
        der.addElement(lineaDer("Comuna: " + valor(colegio != null ? colegio.getComuna() : null)));
        der.addElement(lineaDer("RBD: " + valor(colegio != null ? colegio.getRbd() : null)));
        der.addElement(lineaDer("Año Escolar: " + anio));
        cabecera.addCell(der);

        document.add(cabecera);
    }

    private void escribirCertificado(Document document, Colegio colegio, Curso curso,
                                     Estudiante estudiante, List<CursoAsignatura> asignaturas,
                                     int anio) throws Exception {
        escribirCabecera(document, colegio, anio);

        // Título centrado en negrita.
        Paragraph titulo = new Paragraph("CERTIFICADO ANUAL DE ESTUDIOS",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(14f);
        document.add(titulo);

        Paragraph ensenanza = new Paragraph("ENSEÑANZA BÁSICA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        ensenanza.setAlignment(Element.ALIGN_CENTER);
        document.add(ensenanza);

        Paragraph nombreCurso = new Paragraph(curso.getNombre() != null ? curso.getNombre() : SIN_DATO,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        nombreCurso.setAlignment(Element.ALIGN_CENTER);
        nombreCurso.setSpacingAfter(8f);
        document.add(nombreCurso);

        // Nombre de la escuela y reconocimiento oficial.
        if (colegio != null && colegio.getNombre() != null) {
            Paragraph escuela = new Paragraph(colegio.getNombre(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            escuela.setAlignment(Element.ALIGN_CENTER);
            document.add(escuela);
        }
        if (colegio != null && colegio.getDecreto() != null && !colegio.getDecreto().isBlank()) {
            Paragraph decreto = new Paragraph("Reconocimiento Oficial según " + colegio.getDecreto(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
            decreto.setAlignment(Element.ALIGN_CENTER);
            document.add(decreto);
        }

        // Datos del alumno.
        Paragraph alumno = new Paragraph("Alumno(a): " + nombreCompleto(estudiante),
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        alumno.setSpacingBefore(14f);
        document.add(alumno);

        Paragraph rut = new Paragraph("RUT: " + valor(estudiante.getRut()),
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(rut);

        String fechaNac = estudiante.getFechaNacimiento() != null
                ? estudiante.getFechaNacimiento().format(FECHA)
                : SIN_DATO;
        Paragraph nacimiento = new Paragraph("Fecha de Nacimiento: " + fechaNac,
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        nacimiento.setSpacingAfter(10f);
        document.add(nacimiento);

        // Tabla de calificaciones.
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{3f, 2f, 3f});
        tabla.setHeaderRows(1);
        tabla.addCell(celdaHeader("ASIGNATURA", Element.ALIGN_LEFT));
        tabla.addCell(celdaHeader("CALIFICACIÓN CIFRAS", Element.ALIGN_CENTER));
        tabla.addCell(celdaHeader("CALIFICACIÓN PALABRAS", Element.ALIGN_CENTER));

        List<BigDecimal> notasFinales = new ArrayList<>();
        for (CursoAsignatura ca : asignaturas) {
            if (ca.getAsignatura() == null) {
                continue;
            }
            String nombre = ca.getAsignatura().getNombre() != null
                    ? ca.getAsignatura().getNombre() : SIN_DATO;
            BigDecimal nf = calculoNotasService.notaFinalAnual(
                    estudiante.getId(), ca.getAsignatura().getId(), anio);
            if (nf != null) {
                notasFinales.add(nf);
            }
            tabla.addCell(celdaTexto(nombre, Element.ALIGN_LEFT, false));
            tabla.addCell(celdaTexto(nf != null ? nf.setScale(1, RoundingMode.HALF_UP).toPlainString() : SIN_DATO,
                    Element.ALIGN_CENTER, false));
            tabla.addCell(celdaTexto(notaEnPalabras(nf), Element.ALIGN_CENTER, false));
        }
        document.add(tabla);

        // Promedio general y situación final.
        BigDecimal promedio = promedioGeneral(notasFinales);
        String promedioCifras = promedio != null
                ? promedio.setScale(1, RoundingMode.HALF_UP).toPlainString() : SIN_DATO;
        Paragraph promedioPar = new Paragraph(
                "Promedio General: " + promedioCifras + " (" + notaEnPalabras(promedio) + ")",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
        promedioPar.setSpacingBefore(10f);
        document.add(promedioPar);

        Paragraph situacion = new Paragraph("Situación Final: " + situacionFinal(promedio),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
        document.add(situacion);

        // Campos opcionales en blanco.
        Paragraph religion = new Paragraph("Religión (optativo sin concepto): ______________",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        religion.setSpacingBefore(10f);
        document.add(religion);

        Paragraph asistencia = new Paragraph("ASISTENCIA: ______________",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(asistencia);

        Paragraph observaciones = new Paragraph("Observaciones:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
        observaciones.setSpacingBefore(10f);
        document.add(observaciones);
        document.add(new Paragraph("\n\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));

        // Pie: dos firmas lado a lado.
        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(100);
        firmas.setSpacingBefore(28f);
        firmas.addCell(celdaFirma(nombreProfesorJefe(curso), "Nombre, Apellidos y Firma / Profesor(a) Jefe"));
        firmas.addCell(celdaFirma("", "Nombre, Apellidos y Firma / Director(a)"));
        document.add(firmas);

        String comuna = colegio != null && colegio.getComuna() != null ? colegio.getComuna() : "____________";
        Paragraph lugarFecha = new Paragraph(
                comuna + ", ____ de ____________ de " + anio,
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        lugarFecha.setAlignment(Element.ALIGN_CENTER);
        lugarFecha.setSpacingBefore(20f);
        document.add(lugarFecha);
    }

    /**
     * Convierte una nota (BigDecimal, 1 decimal entre 1.0 y 7.0) a su forma en
     * palabras en español: parte entera ("uno".."siete") + " coma " + dígito
     * decimal ("cero".."nueve"). Ej.: 6.4 -> "seis coma cuatro". null -> "-".
     */
    public String notaEnPalabras(BigDecimal nota) {
        if (nota == null) {
            return SIN_DATO;
        }
        BigDecimal normalizada = nota.setScale(1, RoundingMode.HALF_UP);
        int entero = normalizada.intValue();
        int decimal = normalizada.movePointRight(1).remainder(BigDecimal.TEN).abs().intValue();
        String palabraEntero = (entero >= 0 && entero <= 9) ? UNIDADES[entero] : String.valueOf(entero);
        String palabraDecimal = (decimal >= 0 && decimal <= 9) ? UNIDADES[decimal] : String.valueOf(decimal);
        return palabraEntero + " coma " + palabraDecimal;
    }

    /**
     * Promedio simple (1 decimal, HALF_UP) de las notas finales no nulas.
     * Devuelve null si no hay ninguna.
     */
    BigDecimal promedioGeneral(List<BigDecimal> notasFinales) {
        if (notasFinales == null || notasFinales.isEmpty()) {
            return null;
        }
        BigDecimal suma = notasFinales.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(new BigDecimal(notasFinales.size()), 1, RoundingMode.HALF_UP);
    }

    String situacionFinal(BigDecimal promedio) {
        if (promedio == null) {
            return "PENDIENTE";
        }
        return promedio.compareTo(UMBRAL_APROBACION) >= 0 ? "APROBADO" : "REPROBADO";
    }

    private String nombreCompleto(Estudiante estudiante) {
        String nombre = unirNombre(estudiante.getNombre(), apellidosEstudiante(estudiante));
        return nombre.isEmpty() ? SIN_DATO : nombre;
    }

    private String apellidosEstudiante(Estudiante estudiante) {
        StringBuilder sb = new StringBuilder();
        if (estudiante.getApellidoPaterno() != null && !estudiante.getApellidoPaterno().isBlank()) {
            sb.append(estudiante.getApellidoPaterno().trim());
        }
        if (estudiante.getApellidoMaterno() != null && !estudiante.getApellidoMaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(estudiante.getApellidoMaterno().trim());
        }
        return sb.toString();
    }

    private String nombreProfesorJefe(Curso curso) {
        Profesor jefe = curso.getProfesorJefe();
        if (jefe == null) {
            return "";
        }
        return unirNombre(jefe.getNombre(), jefe.getApellido());
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

    private String valor(String texto) {
        return (texto != null && !texto.isBlank()) ? texto : SIN_DATO;
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

    private Paragraph lineaIzq(String texto, boolean negrita) {
        Font font = negrita
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)
                : FontFactory.getFont(FontFactory.HELVETICA, 8);
        return new Paragraph(texto, font);
    }

    private Paragraph lineaDer(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA, 9));
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
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

    private PdfPCell celdaFirma(String nombre, String rol) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph nombrePar = new Paragraph(
                nombre != null && !nombre.isBlank() ? nombre : " ",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        nombrePar.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(nombrePar);
        Paragraph linea = new Paragraph("____________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        linea.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(linea);
        Paragraph rolPar = new Paragraph(rol, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
        rolPar.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(rolPar);
        return cell;
    }
}
