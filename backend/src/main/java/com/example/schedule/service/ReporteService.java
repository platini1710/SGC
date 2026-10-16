package com.example.schedule.service;

import com.example.schedule.dto.ConsolidadoAnualResponse;
import com.example.schedule.dto.FilaConsolidadoDTO;
import com.example.schedule.dto.FilaNotaDTO;
import com.example.schedule.dto.GrillaNotasResponse;
import com.example.schedule.dto.NotaResponse;
import com.example.schedule.model.Asignatura;
import com.example.schedule.model.Colegio;
import com.example.schedule.model.Curso;
import com.example.schedule.model.Trimestre;
import com.example.schedule.repository.AsignaturaRepository;
import com.example.schedule.repository.ColegioRepository;
import com.example.schedule.repository.CursoRepository;
import com.example.schedule.repository.TrimestreRepository;
import com.example.schedule.security.AuditoriaHelper;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int MAX_NOTAS = 9;
    private static final String SIN_DATO = "-";
    private static final BigDecimal UMBRAL_ROJO = new BigDecimal("4.0");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final NotaService notaService;
    private final NotaAccessChecker notaAccessChecker;
    private final AuditoriaHelper auditoriaHelper;
    private final ColegioRepository colegioRepository;
    private final TrimestreRepository trimestreRepository;
    private final CursoRepository cursoRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final AnioEscolarService anioEscolarService;

    public byte[] generarPdf(Long cursoId, Long asignaturaId, Long trimestreId, Integer anio) {
        notaAccessChecker.verificar(cursoId, asignaturaId);
        if (trimestreId != null) {
            return generarPorTrimestre(cursoId, asignaturaId, trimestreId);
        }
        return generarAnual(cursoId, asignaturaId, anio);
    }

    private byte[] generarPorTrimestre(Long cursoId, Long asignaturaId, Long trimestreId) {
        Trimestre trimestre = trimestreRepository.findById(trimestreId).orElse(null);
        GrillaNotasResponse grilla = notaService.getGrilla(cursoId, asignaturaId, trimestreId);

        Integer numero = trimestre != null ? trimestre.getNumero() : grilla.getNumeroTrimestre();
        Integer anio = trimestre != null ? trimestre.getAnio() : null;
        String subtitulo = subtitulo(grilla.getNombreCurso(), grilla.getNombreAsignatura(),
                numero != null ? "Trimestre " + numero : "Trimestre", anio);

        String[] headers = new String[MAX_NOTAS + 2];
        headers[0] = "Estudiante";
        for (int i = 1; i <= MAX_NOTAS; i++) {
            headers[i] = "N" + i;
        }
        headers[MAX_NOTAS + 1] = "Promedio";

        float[] widths = new float[MAX_NOTAS + 2];
        widths[0] = 4f;
        for (int i = 1; i <= MAX_NOTAS; i++) {
            widths[i] = 1f;
        }
        widths[MAX_NOTAS + 1] = 1.5f;

        return construirPdf(subtitulo, headers, widths, tabla -> {
            for (FilaNotaDTO fila : grilla.getFilas()) {
                tabla.addCell(celdaTexto(fila.getNombreEstudiante(), Element.ALIGN_LEFT, false));
                Map<Integer, BigDecimal> porPosicion = mapearNotas(fila.getNotas());
                for (int pos = 1; pos <= MAX_NOTAS; pos++) {
                    tabla.addCell(celdaValor(porPosicion.get(pos)));
                }
                tabla.addCell(celdaValor(fila.getPromedio()));
            }
        });
    }

    private byte[] generarAnual(Long cursoId, Long asignaturaId, Integer anio) {
        int anioEfectivo = anioEscolarService.resolver(anio);
        ConsolidadoAnualResponse consolidado = notaService.getConsolidado(cursoId, asignaturaId, anioEfectivo);

        String nombreCurso = cursoRepository.findById(cursoId).map(Curso::getNombre).orElse(SIN_DATO);
        String nombreAsignatura =
                asignaturaRepository.findById(asignaturaId).map(Asignatura::getNombre).orElse(SIN_DATO);
        String subtitulo = subtitulo(nombreCurso, nombreAsignatura, "Todos los trimestres", anioEfectivo);

        String[] headers = {"Estudiante", "T1", "T2", "T3", "NF"};
        float[] widths = {4f, 1f, 1f, 1f, 1f};

        return construirPdf(subtitulo, headers, widths, tabla -> {
            for (FilaConsolidadoDTO fila : consolidado.getFilas()) {
                tabla.addCell(celdaTexto(fila.getNombreEstudiante(), Element.ALIGN_LEFT, false));
                tabla.addCell(celdaValor(fila.getPromedioT1()));
                tabla.addCell(celdaValor(fila.getPromedioT2()));
                tabla.addCell(celdaValor(fila.getPromedioT3()));
                tabla.addCell(celdaValor(fila.getNotaFinal()));
            }
        });
    }

    /**
     * Mapea las notas de una fila (por {@link NotaResponse#getPosicion()}, 1..9)
     * a sus valores. Las posiciones ausentes simplemente no aparecen en el mapa,
     * y quien lo consume rinde '-' para esos huecos.
     */
    Map<Integer, BigDecimal> mapearNotas(List<NotaResponse> notas) {
        Map<Integer, BigDecimal> porPosicion = new HashMap<>();
        if (notas == null) {
            return porPosicion;
        }
        for (NotaResponse nota : notas) {
            if (nota.getPosicion() != null) {
                porPosicion.put(nota.getPosicion(), nota.getValor());
            }
        }
        return porPosicion;
    }

    private byte[] construirPdf(String subtitulo, String[] headers, float[] widths,
                                TablaWriter cuerpo) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        try {
            escribirEncabezado(document);

            Paragraph titulo = new Paragraph("Reporte de Notas",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(8f);
            document.add(titulo);

            Paragraph sub = new Paragraph(subtitulo,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(12f);
            document.add(sub);

            PdfPTable tabla = new PdfPTable(headers.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(widths);
            tabla.setHeaderRows(1);
            for (int i = 0; i < headers.length; i++) {
                tabla.addCell(celdaTexto(headers[i], i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER, true));
            }
            cuerpo.write(tabla);
            document.add(tabla);

            escribirPie(document);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando el reporte PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void escribirEncabezado(Document document) {
        Colegio colegio = colegioRepository.findTopByOrderByIdAsc().orElse(null);
        if (colegio == null) {
            return;
        }

        // Encabezado en 2 columnas: datos del colegio (izquierda) + logo (derecha).
        try {
            PdfPTable cabecera = new PdfPTable(2);
            cabecera.setWidthPercentage(100);
            cabecera.setWidths(new float[]{5f, 1.3f});

            // Celda izquierda: nombre y detalle del colegio.
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

            // Celda derecha: logo alineado a la derecha.
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
            // Si algo falla con la tabla/logo, cae a un encabezado solo-texto.
            if (colegio.getNombre() != null) {
                Paragraph nombre = new Paragraph(colegio.getNombre(),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
                nombre.setAlignment(Element.ALIGN_CENTER);
                document.add(nombre);
            }
        }
    }

    /**
     * Carga el logo (data URI Base64) como Image, o null si no es incrustable
     * (SVG, Base64 inválido, formato no soportado). Nunca lanza: el logo no debe
     * hacer fallar el reporte.
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

    private void escribirPie(Document document) {
        Paragraph pie = new Paragraph(
                "Generado el " + LocalDateTime.now().format(FECHA_HORA)
                        + " por " + auditoriaHelper.usuarioActual(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
        pie.setAlignment(Element.ALIGN_RIGHT);
        pie.setSpacingBefore(14f);
        document.add(pie);
    }

    private String subtitulo(String curso, String asignatura, String periodo, Integer anio) {
        String base = curso + " \u00b7 " + asignatura + " \u00b7 " + periodo;
        return anio != null ? base + " \u00b7 " + anio : base;
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

    private PdfPCell celdaTexto(String texto, int alineacion, boolean header) {
        Font font = header
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)
                : FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(4f);
        if (header) {
            cell.setBackgroundColor(new Color(55, 71, 100));
        }
        return cell;
    }

    private PdfPCell celdaValor(BigDecimal valor) {
        String texto = valor != null ? valor.toPlainString() : SIN_DATO;
        boolean rojo = valor != null && valor.compareTo(UMBRAL_ROJO) < 0;
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, rojo ? Color.RED : Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4f);
        return cell;
    }

    @FunctionalInterface
    private interface TablaWriter {
        void write(PdfPTable tabla);
    }
}
