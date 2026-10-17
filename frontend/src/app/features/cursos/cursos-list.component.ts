import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CursoService } from '../../core/services/curso.service';
import { AsignaturaService } from '../../core/services/asignatura.service';
import { ProfesorService } from '../../core/services/profesor.service';
import { PlanEstudioService } from '../../core/services/plan-estudio.service';
import { Curso, CursoDetalle, CursoAsignaturaRequest } from '../../core/models/curso.model';
import { Asignatura } from '../../core/models/asignatura.model';
import { Profesor } from '../../core/models/profesor.model';
import { PlanEstudio } from '../../core/models/plan-estudio.model';

@Component({
  selector: 'app-cursos-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './cursos-list.component.html',
  styleUrl: './cursos-list.component.scss'
})
export class CursosListComponent implements OnInit {
  private cursoService = inject(CursoService);
  private asignaturaService = inject(AsignaturaService);
  private profesorService = inject(ProfesorService);
  private planEstudioService = inject(PlanEstudioService);
  private fb = inject(FormBuilder);

  cursos: Curso[] = [];
  asignaturas: Asignatura[] = [];
  asignaturasDisponibles: Asignatura[] = [];
  profesores: Profesor[] = [];
  planes: PlanEstudio[] = [];
  cursoSeleccionado: CursoDetalle | null = null;
  form!: FormGroup;
  mostrarForm = false;
  cargando = false;
  errorMsg = '';
  mensajeOk = '';

  ngOnInit(): void {
    this.initForm();
    this.cargar();
    this.asignaturaService.getAll().subscribe(a => (this.asignaturas = a));
    this.profesorService.getAll().subscribe(p => (this.profesores = p));
    this.planEstudioService.getAll().subscribe(p => (this.planes = p));
  }

  private initForm(): void {
    this.form = this.fb.group({
      asignaturaId: [null as number | null, Validators.required],
      horasSemanales: [1, [Validators.required, Validators.min(1)]],
      profesorId: [null as number | null]
    });
  }

  cargar(): void {
    this.cargando = true;
    this.cursoService.getAll().subscribe({
      next: (c) => { this.cursos = c; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando cursos'; this.cargando = false; }
    });
  }

  gestionar(curso: Curso): void {
    this.mensajeOk = '';
    this.errorMsg = '';
    this.mostrarForm = false;
    this.cargarDetalle(curso.id);
  }

  private cargarDetalle(cursoId: number): void {
    this.cursoService.getById(cursoId).subscribe({
      next: (d) => { this.cursoSeleccionado = d; this.onDetalleCargado(d); this.cargarAsignaturasDisponibles(cursoId); },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error cargando detalle del curso')
    });
  }

  private cargarAsignaturasDisponibles(cursoId: number): void {
    this.cursoService.getAsignaturasDisponibles(cursoId).subscribe({
      next: (a) => (this.asignaturasDisponibles = a),
      error: (e) => { this.asignaturasDisponibles = []; this.errorMsg = e?.error?.error ?? 'Error cargando asignaturas disponibles'; }
    });
  }

  cerrarPanel(): void {
    this.cursoSeleccionado = null;
    this.mostrarForm = false;
    this.asignaturasDisponibles = [];
    this.planEstudioSeleccionado = null;
    this.form.reset({ asignaturaId: null, horasSemanales: 1, profesorId: null });
  }

  abrirForm(): void {
    this.mostrarForm = true;
    this.form.reset({ asignaturaId: null, horasSemanales: 1, profesorId: null });
  }

  cancelarForm(): void {
    this.mostrarForm = false;
    this.form.reset({ asignaturaId: null, horasSemanales: 1, profesorId: null });
  }

  agregarAsignatura(): void {
    if (this.form.invalid || !this.cursoSeleccionado) { return; }
    const cursoId = this.cursoSeleccionado.id;
    const val = this.form.value as { asignaturaId: number; horasSemanales: number; profesorId: number | null };
    const req: CursoAsignaturaRequest = {
      asignaturaId: val.asignaturaId,
      horasSemanales: val.horasSemanales,
      profesorId: val.profesorId
    };
    this.cursoService.addAsignatura(cursoId, req).subscribe({
      next: () => { this.mensajeOk = 'Asignatura asignada.'; this.cancelarForm(); this.cargarDetalle(cursoId); },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error asignando asignatura')
    });
  }

  quitarAsignatura(asignaturaId: number): void {
    if (!this.cursoSeleccionado) { return; }
    if (!window.confirm('¿Quitar esta asignatura del curso?')) { return; }
    const cursoId = this.cursoSeleccionado.id;
    this.cursoService.removeAsignatura(cursoId, asignaturaId).subscribe({
      next: () => { this.mensajeOk = 'Asignatura quitada.'; this.cargarDetalle(cursoId); },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error quitando asignatura')
    });
  }

  // Profesor jefe seleccionado en el dropdown (ngModel).
  profesorJefeSeleccionado: number | null = null;

  // Plan de estudio seleccionado en el dropdown (ngModel).
  planEstudioSeleccionado: number | null = null;

  // Al cargar el detalle, sincroniza el profesor jefe y el plan actuales con los selectores.
  onDetalleCargado(d: CursoDetalle): void {
    this.profesorJefeSeleccionado = d.profesorJefeId ?? null;
    this.planEstudioSeleccionado = d.planEstudioId ?? null;
  }

  guardarProfesorJefe(): void {
    if (!this.cursoSeleccionado) { return; }
    const cursoId = this.cursoSeleccionado.id;
    this.cursoService.asignarProfesorJefe(cursoId, this.profesorJefeSeleccionado).subscribe({
      next: () => {
        this.mensajeOk = 'Profesor jefe actualizado.';
        this.cargarDetalle(cursoId);
        this.cargar(); // refresca la lista (muestra el jefe en la tabla)
      },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error asignando profesor jefe')
    });
  }

  guardarPlanEstudio(): void {
    if (!this.cursoSeleccionado) { return; }
    const cursoId = this.cursoSeleccionado.id;
    this.cursoService.asignarPlanEstudio(cursoId, this.planEstudioSeleccionado).subscribe({
      next: () => {
        this.mensajeOk = 'Plan de estudio actualizado.';
        this.cargarDetalle(cursoId); // recarga asignaturas disponibles según el nuevo plan
        this.cargar();
      },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error asignando plan de estudio')
    });
  }
}
