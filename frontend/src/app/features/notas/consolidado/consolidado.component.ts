import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CursoService } from '../../../core/services/curso.service';
import { AsignaturaService } from '../../../core/services/asignatura.service';
import { NotaService } from '../../../core/services/nota.service';
import { AuthService } from '../../../core/services/auth.service';
import { Curso } from '../../../core/models/curso.model';
import { Asignatura } from '../../../core/models/asignatura.model';
import { ConsolidadoAnual, AsignacionDocente } from '../../../core/models/nota.model';

@Component({
  selector: 'app-consolidado',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './consolidado.component.html',
  styleUrl: './consolidado.component.scss'
})
export class ConsolidadoComponent implements OnInit {
  private cursoService = inject(CursoService);
  private asignaturaService = inject(AsignaturaService);
  private notaService = inject(NotaService);
  private authService = inject(AuthService);

  esDocente = this.authService.getRol() === 'DOCENTE';
  private misAsignaciones: AsignacionDocente[] = [];

  cursos: Curso[] = [];
  asignaturas: Asignatura[] = [];

  cursoId: number | null = null;
  asignaturaId: number | null = null;
  anio = new Date().getFullYear();

  consolidado: ConsolidadoAnual | null = null;

  cargando = false;
  errorMsg = '';

  ngOnInit(): void {
    if (this.esDocente) {
      // DOCENTE: cursos y asignaturas provienen de sus asignaciones (sin endpoints de administración).
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
  }

  // Para DOCENTE: filtra las asignaturas según el curso elegido (o todas las suyas si no hay curso).
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

  onSeleccion(): void {
    this.errorMsg = '';
    if (this.esDocente) {
      this.actualizarAsignaturasDocente();
    }
    if (this.cursoId != null && this.asignaturaId != null && this.anio != null) {
      this.cargar();
    } else {
      this.consolidado = null;
    }
  }

  private cargar(): void {
    if (this.cursoId == null || this.asignaturaId == null || this.anio == null) { return; }
    this.cargando = true;
    this.notaService.getConsolidado(this.cursoId, this.asignaturaId, this.anio).subscribe({
      next: (c) => { this.consolidado = c; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando el consolidado'; this.cargando = false; }
    });
  }
}
