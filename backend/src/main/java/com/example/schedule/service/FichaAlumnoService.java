package com.example.schedule.service;

import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.model.Apoderado;
import com.example.schedule.model.Colegio;
import com.example.schedule.model.Estudiante;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.EstudianteRepository;
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
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Genera la "Ficha de Matrícula y Antecedentes del Alumno" (una página A4
 * vertical por alumno). Es un reporte NUEVO y aislado de los reportes por
 * asignatura ({@link ReporteService}), del boletín por alumno
 * ({@link InformeAlumnoService}) y del certificado de estudios
 * ({@link CertificadoService}); replica el patrón de este último (cargarLogo,
 * helpers de celda, barra de título azul) sin reutilizar sus métodos privados.
 */
@Service
@RequiredArgsConstructor
public class FichaAlumnoService {

    private static final String SIN_DATO = "—";
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final EstudianteRepository estudianteRepository;
    private final ColegioRepository colegioRepository;
    private final NotaAccessChecker notaAccessChecker;

    public byte[] generarFicha(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con id: " + estudianteId));

        notaAccessChecker.verificarCurso(
                estudiante.getCurso() != null ? estudiante.getCurso().getId() : null);

        Colegio colegio = colegioRepository.findTopByOrderByIdAsc().orElse(null);

        Document document = new Document(PageSize.A4, 40, 40, 40, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        try {
            escribirFicha(document, colegio, estudiante);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando la ficha PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void escribirFicha(Document document, Colegio colegio, Estudiante estudiante) throws Exception {
        // Encabezado: izquierda logo + nombre del colegio; derecha recuadro foto.
        PdfPTable encabezado = new PdfPTable(2);
        encabezado.setWidthPercentage(100);
        encabezado.setWidths(new float[]{3.2f, 1.6f});

        PdfPCell izq = new PdfPCell();
        izq.setBorder(0);
        Image logo = cargarLogo(colegio != null ? colegio.getLogo() : null);
        if (logo != null) {
            logo.scaleToFit(60, 60);
            Paragraph logoPar = new Paragraph();
            logoPar.add(new com.lowagie.text.Chunk(logo, 0, 0));
            izq.addElement(logoPar);
        }
        if (colegio != null && colegio.getNombre() != null && !colegio.getNombre().isBlank()) {
            Paragraph nombreColegio = new Paragraph(colegio.getNombre(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            izq.addElement(nombreColegio);
        }
        encabezado.addCell(izq);

        // Recuadro FOTOGRAFIA DEL ALUMNO.
        PdfPCell fotoCell = new PdfPCell();
        fotoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        fotoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fotoCell.setMinimumHeight(120f);
        fotoCell.setPadding(4f);
        Image foto = cargarFoto(estudiante.getFoto());
        if (foto != null) {
            foto.scaleToFit(90, 110);
            fotoCell.addElement(new com.lowagie.text.Chunk(foto, 0, 0));
        } else {
            Paragraph texto = new Paragraph("FOTOGRAFÍA DEL ALUMNO",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            texto.setAlignment(Element.ALIGN_CENTER);
            fotoCell.addElement(texto);
        }
        encabezado.addCell(fotoCell);
        document.add(encabezado);

        // Título centrado en negrita.
        Paragraph titulo = new Paragraph("Ficha de Matrícula y Antecedentes del Alumno",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(14f);
        titulo.setSpacingAfter(10f);
        document.add(titulo);

        // Sección I: Identificación.
        document.add(barraTitulo("I. IDENTIFICACIÓN DEL ALUMNO"));
        PdfPTable identificacion = tablaDatos();
        agregarDato(identificacion, "Apellidos", valor(estudiante.getApellido()));
        agregarDato(identificacion, "Nombres", valor(estudiante.getNombre()));
        agregarDato(identificacion, "RUN / DNI", valor(estudiante.getRut()));
        agregarDato(identificacion, "Fecha de Nacimiento",
                estudiante.getFechaNacimiento() != null
                        ? estudiante.getFechaNacimiento().format(FECHA) : SIN_DATO);
        agregarDato(identificacion, "Lugar de Nacimiento", valor(estudiante.getLugarNacimiento()));
        agregarDato(identificacion, "Nacionalidad", valor(estudiante.getNacionalidad()));
        agregarDato(identificacion, "Sexo", valor(estudiante.getSexo()));
        agregarDato(identificacion, "Edad", edad(estudiante.getFechaNacimiento()));
        agregarDato(identificacion, "Comuna", valor(estudiante.getComuna()));
        agregarDato(identificacion, "Teléfono", valor(estudiante.getTelefono()));
        agregarDato(identificacion, "Domicilio", valor(estudiante.getDomicilio()));
        agregarDato(identificacion, "Curso",
                estudiante.getCurso() != null ? valor(estudiante.getCurso().getNombre()) : SIN_DATO);
        document.add(identificacion);

        // Sección II: Antecedentes familiares.
        document.add(barraTitulo("II. ANTECEDENTES FAMILIARES"));
        PdfPTable familiares = tablaDatos();
        agregarDato(familiares, "Padre - Nombre", valor(estudiante.getPadreNombre()));
        agregarDato(familiares, "Padre - RUN", valor(estudiante.getPadreRut()));
        agregarDato(familiares, "Padre - Ocupación", valor(estudiante.getPadreOcupacion()));
        agregarDato(familiares, "Madre - Nombre", valor(estudiante.getMadreNombre()));
        agregarDato(familiares, "Madre - RUN", valor(estudiante.getMadreRut()));
        agregarDato(familiares, "Madre - Ocupación", valor(estudiante.getMadreOcupacion()));
        agregarDato(familiares, "Apoderado", nombreApoderado(estudiante.getApoderado()));
        agregarDato(familiares, "Relación", valor(estudiante.getApoderadoRelacion()));
        agregarDato(familiares, "Correo Apoderado",
                estudiante.getApoderado() != null ? valor(estudiante.getApoderado().getCorreo()) : SIN_DATO);
        document.add(familiares);

        // Sección III: Información médica.
        document.add(barraTitulo("III. INFORMACIÓN MÉDICA"));
        PdfPTable medica = tablaDatos();
        agregarDato(medica, "Previsión", valor(estudiante.getPrevision()));
        agregarDato(medica, "Alergias", valor(estudiante.getAlergias()));
        agregarDato(medica, "Condición Médica", valor(estudiante.getCondicionMedica()));
        agregarDato(medica, "Contacto Emergencia", valor(estudiante.getContactoEmergencia()));
        agregarDato(medica, "Fono Emergencia", valor(estudiante.getFonoEmergencia()));
        document.add(medica);

        // Pie: dos firmas lado a lado.
        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(100);
        firmas.setSpacingBefore(40f);
        firmas.addCell(celdaFirma("FIRMA APODERADO"));
        firmas.addCell(celdaFirma("FIRMA SECRETARIA ADMISIONES"));
        document.add(firmas);
    }

    /**
     * Edad en años cumplidos vía {@link Period#between}. null -> "—".
     */
    private String edad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return SIN_DATO;
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears() + " años";
    }

    private String nombreApoderado(Apoderado apoderado) {
        if (apoderado == null) {
            return SIN_DATO;
        }
        StringBuilder sb = new StringBuilder();
        if (apoderado.getNombre() != null && !apoderado.getNombre().isBlank()) {
            sb.append(apoderado.getNombre().trim());
        }
        if (apoderado.getApellidoPaterno() != null && !apoderado.getApellidoPaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(apoderado.getApellidoPaterno().trim());
        }
        return sb.length() == 0 ? SIN_DATO : sb.toString();
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

    /**
     * Carga la foto del alumno (data URI Base64) como Image, o null si no es
     * incrustable (SVG, Base64 inválido, formato no soportado). Nunca lanza.
     */
    private Image cargarFoto(String foto) {
        try {
            if (foto == null || !foto.startsWith("data:image") || foto.contains("svg")) {
                return null;
            }
            int coma = foto.indexOf(',');
            if (coma < 0) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(foto.substring(coma + 1).trim());
            return Image.getInstance(bytes);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Barra de título de sección: celda con fondo azul y texto blanco, igual
     * que el celdaHeader de CertificadoService.
     */
    private PdfPTable barraTitulo(String texto) {
        PdfPTable barra = new PdfPTable(1);
        barra.setWidthPercentage(100);
        barra.setSpacingBefore(12f);
        barra.setSpacingAfter(4f);
        PdfPCell cell = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5f);
        cell.setBackgroundColor(new Color(55, 71, 100));
        barra.addCell(cell);
        return barra;
    }

    private PdfPTable tablaDatos() {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        try {
            tabla.setWidths(new float[]{1.4f, 3f});
        } catch (Exception ignore) {
            // widths por defecto si falla
        }
        return tabla;
    }

    private void agregarDato(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell etiquetaCell = new PdfPCell(new Phrase(etiqueta,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        etiquetaCell.setPadding(4f);
        etiquetaCell.setBackgroundColor(new Color(240, 242, 245));
        tabla.addCell(etiquetaCell);

        PdfPCell valorCell = new PdfPCell(new Phrase(valor,
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        valorCell.setPadding(4f);
        tabla.addCell(valorCell);
    }

    private PdfPCell celdaFirma(String rol) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph linea = new Paragraph("____________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        linea.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(linea);
        Paragraph rolPar = new Paragraph(rol,
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
        rolPar.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(rolPar);
        return cell;
    }
}
