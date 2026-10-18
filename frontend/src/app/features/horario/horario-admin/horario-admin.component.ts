import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HorarioService } from '../../../core/services/horario.service';
import { CursoService } from '../../../core/services/curso.service';
import { Curso } from '../../../core/models/curso.model';

@Component({
  selector: 'app-horario-admin',
  standalone: true,
  imports: [RouterModule, FormsModule],
  templateUrl: './horario-admin.component.html',
  styleUrl: './horario-admin.component.scss'
})
export class HorarioAdminComponent implements OnInit {
  private horarioService = inject(HorarioService);
  private cursoService = inject(CursoService);

  generando = false;
  borrando = false;
  mensajeGenerar = '';
  mensajeBorrar = '';
  errorGenerar = '';
  errorBorrar = '';

  // Generación por curso.
  cursos: Curso[] = [];
  cursoSeleccionadoId: number | null = null;
  generandoCurso = false;
  mensajeGenerarCurso = '';
  errorGenerarCurso = '';

  ngOnInit(): void {
    this.cursoService.getAll().subscribe({
      next: (cursos) => { this.cursos = cursos; },
      error: () => { this.errorGenerarCurso = 'No se pudieron cargar los cursos.'; }
    });
  }

  generarHorarioCurso(): void {
    if (this.cursoSeleccionadoId == null) { return; }
    const cursoId = Number(this.cursoSeleccionadoId);
    const nombreCurso = this.cursos.find(c => c.id === cursoId)?.nombre ?? '';
    this.generandoCurso = true;
    this.mensajeGenerarCurso = '';
    this.errorGenerarCurso = '';
    this.horarioService.generarHorarioCurso(cursoId).subscribe({
      next: () => {
        this.generandoCurso = false;
        this.mensajeGenerarCurso = `✅ Horario del curso ${nombreCurso} generado.`;
      },
      error: (err) => {
        this.generandoCurso = false;
        this.errorGenerarCurso = `Error al generar horario del curso: ${err?.error?.error ?? err.message ?? 'Error desconocido'}`;
      }
    });
  }

  generarHorario(): void {
    this.generando = true;
    this.mensajeGenerar = '';
    this.errorGenerar = '';
    this.horarioService.generarHorario().subscribe({
      next: () => {
        this.generando = false;
        this.mensajeGenerar = '✅ Horario generado exitosamente.';
      },
      error: (err) => {
        this.generando = false;
        this.errorGenerar = `Error al generar horario: ${err.message ?? 'Error desconocido'}`;
      }
    });
  }

  borrarHorario(): void {
    const confirm = window.confirm('¿Está seguro que desea borrar todos los horarios? Esta acción no se puede deshacer.');
    if (!confirm) { return; }
    this.borrando = true;
    this.mensajeBorrar = '';
    this.errorBorrar = '';
    this.horarioService.borrarHorario().subscribe({
      next: () => {
        this.borrando = false;
        this.mensajeBorrar = '✅ Horarios borrados exitosamente.';
      },
      error: (err) => {
        this.borrando = false;
        this.errorBorrar = `Error al borrar horario: ${err.message ?? 'Error desconocido'}`;
      }
    });
  }
}
