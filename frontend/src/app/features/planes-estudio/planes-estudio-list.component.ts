import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PlanEstudioService } from '../../core/services/plan-estudio.service';
import { AsignaturaService } from '../../core/services/asignatura.service';
import { PlanEstudio, PlanEstudioDetalle, PlanEstudioRequest } from '../../core/models/plan-estudio.model';
import { Asignatura } from '../../core/models/asignatura.model';

@Component({
  selector: 'app-planes-estudio-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './planes-estudio-list.component.html',
  styleUrl: './planes-estudio-list.component.scss'
})
export class PlanesEstudioListComponent implements OnInit {
  private planService = inject(PlanEstudioService);
  private asignaturaService = inject(AsignaturaService);
  private fb = inject(FormBuilder);

  planes: PlanEstudio[] = [];
  asignaturas: Asignatura[] = [];
  planSeleccionado: PlanEstudioDetalle | null = null;
  form!: FormGroup;
  mostrarForm = false;
  editandoId: number | null = null;
  cargando = false;
  errorMsg = '';
  mensajeOk = '';

  // Asignatura a agregar a la malla (ngModel en el panel de detalle).
  asignaturaSeleccionada: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.cargar();
    this.asignaturaService.getAll().subscribe(a => (this.asignaturas = a));
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      descripcion: [''],
      decreto: [''],
      nivelDesde: [1, [Validators.required, Validators.min(1)]],
      nivelHasta: [1, [Validators.required, Validators.min(1)]],
      activo: [true]
    });
  }

  cargar(): void {
    this.cargando = true;
    this.planService.getAll().subscribe({
      next: (p) => { this.planes = p; this.cargando = false; },
      error: (e) => { this.errorMsg = e?.error?.error ?? 'Error cargando planes de estudio'; this.cargando = false; }
    });
  }

  abrirForm(p?: PlanEstudio): void {
    this.mostrarForm = true;
    this.mensajeOk = '';
    this.errorMsg = '';
    if (p) {
      this.editandoId = p.id;
      this.form.setValue({
        nombre: p.nombre,
        descripcion: p.descripcion ?? '',
        decreto: p.decreto ?? '',
        nivelDesde: p.nivelDesde,
        nivelHasta: p.nivelHasta,
        activo: p.activo
      });
    } else {
      this.editandoId = null;
      this.form.reset({ nombre: '', descripcion: '', decreto: '', nivelDesde: 1, nivelHasta: 1, activo: true });
    }
  }

  cancelar(): void {
    this.mostrarForm = false;
    this.editandoId = null;
    this.form.reset({ nombre: '', descripcion: '', decreto: '', nivelDesde: 1, nivelHasta: 1, activo: true });
  }

  guardar(): void {
    if (this.form.invalid) { return; }
    const val = this.form.value as PlanEstudioRequest;
    const req: PlanEstudioRequest = {
      nombre: val.nombre,
      descripcion: val.descripcion,
      decreto: val.decreto,
      nivelDesde: val.nivelDesde,
      nivelHasta: val.nivelHasta,
      activo: val.activo
    };
    if (this.editandoId !== null) {
      this.planService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = 'Plan de estudio actualizado.'; this.cancelar(); this.cargar(); },
        error: (e) => (this.errorMsg = e?.error?.error ?? 'Error actualizando plan de estudio')
      });
    } else {
      this.planService.create(req).subscribe({
        next: () => { this.mensajeOk = 'Plan de estudio creado.'; this.cancelar(); this.cargar(); },
        error: (e) => (this.errorMsg = e?.error?.error ?? 'Error creando plan de estudio')
      });
    }
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Eliminar este plan de estudio?')) { return; }
    this.planService.delete(id).subscribe({
      next: () => {
        this.mensajeOk = 'Plan de estudio eliminado.';
        if (this.planSeleccionado?.id === id) { this.planSeleccionado = null; }
        this.cargar();
      },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error eliminando plan de estudio')
    });
  }

  gestionar(plan: PlanEstudio): void {
    this.mensajeOk = '';
    this.errorMsg = '';
    this.mostrarForm = false;
    this.cargarDetalle(plan.id);
  }

  private cargarDetalle(planId: number): void {
    this.planService.getById(planId).subscribe({
      next: (d) => { this.planSeleccionado = d; this.asignaturaSeleccionada = null; },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error cargando detalle del plan')
    });
  }

  cerrarPanel(): void {
    this.planSeleccionado = null;
    this.asignaturaSeleccionada = null;
  }

  agregarAsignatura(): void {
    if (!this.planSeleccionado || this.asignaturaSeleccionada == null) { return; }
    const planId = this.planSeleccionado.id;
    this.planService.addAsignatura(planId, this.asignaturaSeleccionada).subscribe({
      next: () => { this.mensajeOk = 'Asignatura agregada al plan.'; this.cargarDetalle(planId); this.cargar(); },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error agregando asignatura al plan')
    });
  }

  quitarAsignatura(asignaturaId: number): void {
    if (!this.planSeleccionado) { return; }
    if (!window.confirm('¿Quitar esta asignatura del plan?')) { return; }
    const planId = this.planSeleccionado.id;
    this.planService.removeAsignatura(planId, asignaturaId).subscribe({
      next: () => { this.mensajeOk = 'Asignatura quitada del plan.'; this.cargarDetalle(planId); this.cargar(); },
      error: (e) => (this.errorMsg = e?.error?.error ?? 'Error quitando asignatura del plan')
    });
  }
}
