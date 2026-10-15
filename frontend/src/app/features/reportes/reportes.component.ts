import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CursoService } from '../../core/services/curso.service';
import { AsignaturaService } from '../../core/services/asignatura.service';
import { NotaService } from '../../core/services/nota.service';
import { ReporteService } from '../../core/services/reporte.service';
import { TrimestreService } from '../../core/services/trimestre.service';
import { AuthService } from '../../core/services/auth.service';
import { Curso } from '../../core/models/curso.model';
import { Asignatura } from '../../core/models/asignatura.model';
import { Trimestre } from '../../core/models/trimestre.model';
import { AsignacionDocente } from '../../core/models/nota.model';
import { Estudiante } from '../../core/models/estudiante.model';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.scss'
})
export class ReportesComponent implements OnInit, OnDestroy {
  private cursoService = inject(CursoService);
  private asignaturaService = inject(AsignaturaService);
  private notaService = inject(NotaService);
  private reporteService = inject(ReporteService);
  private trimestreService = inject(TrimestreService);
  private authService = inject(AuthService);
  private sanitizer = inject(DomSanitizer);

  esDocente = this.authService.getRol() === 'DOCENTE';
  private misAsignaciones: AsignacionDocente[] = [];

  cursos: Curso[] = [];
  asignaturas: Asignatura[] = [];
  trimestres: Trimestre[] = [];

  cursoId: number | null = null;
  asignaturaId: number | null = null;
  // 'todos' = reporte anual; o el id del trimestre específico.
  alcance: 'todos' | number = 'todos';

  // Tipo de reporte: 'asignatura' (el existente) o 'alumno' (boletín por alumno).
  tipoReporte: 'asignatura' | 'alumno' = 'asignatura';
  // Estado del modo "Informe por alumno".
  trimestreIdAlumno: number | null = null;
  estudianteId: number | null = null; // null = "Todos los alumnos del curso".
  estudiantesCurso: Estudiante[] = [];

  generando = false;
  errorMsg = '';
  mensajeOk = '';

  // Vista previa del PDF generado (antes de guardar en disco).
  private pdfBlob: Blob | null = null;
  private pdfObjectUrl: string | null = null;
  pdfPreviewUrl: SafeResourceUrl | null = null;
  private nombreArchivo = 'reporte-notas.pdf';

  ngOnInit(): void {
    if (this.esDocente) {
      this.notaService.getMisAsignaciones().subscribe({
        next: (asignaciones) => {
          this.misAsignaciones = asignaciones;
          const porCurso = new Map<number, string>();
          for (const a of asignaciones) {
            if (!porCurso.has(a.cursoId)) { porCurso.set(a.cursoId, a.nombreCurso); }
          }
          this.cursos = Array.from(porCurso.entries())
            .map(([id, nombre]) => ({ id, nombre, nivel: 0 }));
          this.actualizarAsignaturasDocente();
        },
        error: () => (this.errorMsg = 'Error cargando sus asignaciones')
      });
    } else {
      this.cursoService.getAll().subscribe({
        next: (c) => (this.cursos = c),
        error: () => (this.errorMsg = 'Error cargando cursos')
      });
      this.asignaturaService.getAll().subscribe({
        next: (a) => (this.asignaturas = a),
        error: () => (this.errorMsg = 'Error cargando asignaturas')
      });
    }

    this.trimestreService.getAll().subscribe({
      next: (t) => (this.trimestres = t),
      error: () => { /* el selector de alcance funciona igual con "todos" */ }
    });
  }

  // Para DOCENTE: filtra las asignaturas según el curso elegido.
  private actualizarAsignaturasDocente(): void {
    const fuente = this.cursoId == null
      ? this.misAsignaciones
      : this.misAsignaciones.filter((a) => a.cursoId === this.cursoId);
    const porAsig = new Map<number, string>();
    for (const a of fuente) {
      if (!porAsig.has(a.asignaturaId)) { porAsig.set(a.asignaturaId, a.nombreAsignatura); }
    }
    this.asignaturas = Array.from(porAsig.entries())
      .map(([id, nombre]) => ({ id, nombre, codigo: '', descripcion: '' }));
  }

  onCursoChange(): void {
    this.errorMsg = '';
    this.mensajeOk = '';
    this.asignaturaId = null;
    if (this.esDocente) {
      this.actualizarAsignaturasDocente();
    }
  }

  get puedeGenerar(): boolean {
    return !this.generando && this.cursoId != null && this.asignaturaId != null;
  }

  // Modo "Informe por alumno": al cambiar el curso, resetea el alumno y carga la lista.
  onCursoAlumnoChange(): void {
    this.errorMsg = '';
    this.mensajeOk = '';
    this.estudianteId = null;
    this.estudiantesCurso = [];
    if (this.cursoId == null) { return; }
    this.cursoService.getEstudiantes(this.cursoId).subscribe({
      next: (p) => (this.estudiantesCurso = p.content),
      error: () => (this.errorMsg = 'Error cargando alumnos del curso')
    });
  }

  get puedeGenerarAlumno(): boolean {
    return !this.generando && this.cursoId != null && this.trimestreIdAlumno != null;
  }

  // Genera el boletín por alumno y lo muestra embebido como vista previa.
  generarAlumno(): void {
    if (!this.puedeGenerarAlumno || this.cursoId == null || this.trimestreIdAlumno == null) { return; }
    this.generando = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    this.limpiarPreview();

    const sufijo = this.estudianteId != null ? '-alumno' + this.estudianteId : '';
    this.nombreArchivo = `informe-notas-curso${this.cursoId}-T${this.trimestreIdAlumno}${sufijo}.pdf`;

    this.reporteService.descargarInformeAlumno(this.cursoId, this.trimestreIdAlumno, this.estudianteId).subscribe({
      next: (blob) => {
        this.generando = false;
        this.pdfBlob = blob;
        this.pdfObjectUrl = window.URL.createObjectURL(blob);
        this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl + '#view=FitH');
        this.mensajeOk = 'Vista previa generada. Puede guardarla en disco con el botón "Descargar".';
      },
      error: (err: HttpErrorResponse) => {
        this.generando = false;
        if (err.status === 403) {
          this.errorMsg = 'No tiene acceso a este curso.';
        } else {
          this.errorMsg = 'Error generando el informe.';
        }
      }
    });
  }

  // Genera el PDF y lo muestra embebido como vista previa (sin descargar aún).
  generar(): void {
    if (!this.puedeGenerar || this.cursoId == null || this.asignaturaId == null) { return; }
    this.generando = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    this.limpiarPreview();

    const trimestreId = this.alcance === 'todos' ? null : this.alcance;
    const periodo = trimestreId == null ? 'anual' : 'T' + trimestreId;
    this.nombreArchivo = `reporte-notas-${this.cursoId}-${this.asignaturaId}-${periodo}.pdf`;

    this.reporteService.descargarPdf(this.cursoId, this.asignaturaId, trimestreId).subscribe({
      next: (blob) => {
        this.generando = false;
        this.pdfBlob = blob;
        this.pdfObjectUrl = window.URL.createObjectURL(blob);
        // Añade #toolbar=1 para mostrar la barra del visor nativo del navegador.
        this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl + '#view=FitH');
        this.mensajeOk = 'Vista previa generada. Puede guardarla en disco con el botón "Descargar".';
      },
      error: (err: HttpErrorResponse) => {
        this.generando = false;
        if (err.status === 403) {
          this.errorMsg = 'No tiene acceso a esa asignatura.';
        } else {
          this.errorMsg = 'Error generando el reporte.';
        }
      }
    });
  }

  // Guarda en disco el PDF ya generado en la vista previa.
  descargar(): void {
    if (!this.pdfBlob) { return; }
    const url = window.URL.createObjectURL(this.pdfBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = this.nombreArchivo;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  // Al cambiar el tipo de reporte, limpia la vista previa y los mensajes
  // para no mostrar un PDF del tipo anterior.
  onTipoChange(): void {
    this.limpiarPreview();
    this.mensajeOk = '';
    this.errorMsg = '';
  }

  get hayPreview(): boolean {
    return this.pdfPreviewUrl != null;
  }

  private limpiarPreview(): void {
    if (this.pdfObjectUrl) {
      window.URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
    }
    this.pdfBlob = null;
    this.pdfPreviewUrl = null;
  }

  ngOnDestroy(): void {
    this.limpiarPreview();
  }
}
