import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProfesorService } from '../../core/services/profesor.service';
import { EspecialidadService } from '../../core/services/especialidad.service';
import { Profesor } from '../../core/models/profesor.model';
import { Especialidad } from '../../core/models/especialidad.model';

@Component({
  selector: 'app-profesores-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profesores-list.component.html',
  styleUrl: './profesores-list.component.scss'
})
export class ProfesoresListComponent implements OnInit {
  private profesorService = inject(ProfesorService);
  private especialidadService = inject(EspecialidadService);
  private fb = inject(FormBuilder);

  profesores: Profesor[] = [];
  especialidades: Especialidad[] = [];
  form!: FormGroup;
  mostrarForm = false;
  editandoId: number | null = null;
  cargando = false;
  errorMsg = '';
  mensajeOk = '';

  ngOnInit(): void {
    this.initForm();
    this.cargar();
    this.especialidadService.getAll().subscribe(e => (this.especialidades = e));
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      // RF-01.4: correo obligatorio y con formato válido.
      email: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)]],
      rut: [''],
      especialidadIds: [[]]
    });
  }

  cargar(): void {
    this.cargando = true;
    this.profesorService.getAll().subscribe({
      next: (p) => { this.profesores = p; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando profesores'; this.cargando = false; }
    });
  }

  abrirForm(p?: Profesor): void {
    this.mostrarForm = true;
    this.mensajeOk = '';
    this.errorMsg = '';
    if (p) {
      this.editandoId = p.id;
      this.form.setValue({
        nombre: p.nombre,
        apellido: p.apellido,
        email: p.email ?? '',
        rut: p.rut ?? '',
        especialidadIds: []
      });
    } else {
      this.editandoId = null;
      this.form.reset({ nombre: '', apellido: '', email: '', rut: '', especialidadIds: [] });
    }
  }

  cancelar(): void {
    this.mostrarForm = false;
    this.editandoId = null;
    this.form.reset();
  }

  get email() { return this.form.get('email')!; }

  guardar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const val = this.form.value as { nombre: string; apellido: string; email: string; rut: string; especialidadIds: number[] };
    const req = { nombre: val.nombre, apellido: val.apellido, email: val.email, rut: val.rut, especialidadIds: val.especialidadIds ?? [] };
    if (this.editandoId !== null) {
      this.profesorService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = 'Profesor actualizado.'; this.cancelar(); this.cargar(); },
        error: (err) => (this.errorMsg = this.mensajeError(err, 'Error actualizando profesor'))
      });
    } else {
      this.profesorService.create(req).subscribe({
        next: () => { this.mensajeOk = 'Profesor creado.'; this.cancelar(); this.cargar(); },
        error: (err) => (this.errorMsg = this.mensajeError(err, 'Error creando profesor'))
      });
    }
  }

  // Usa el mensaje del backend (ej. 409 correo duplicado, 400 validación) si viene.
  private mensajeError(err: unknown, fallback: string): string {
    const e = err as { error?: { error?: string } };
    return e?.error?.error ?? fallback;
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Eliminar este profesor?')) { return; }
    this.profesorService.delete(id).subscribe({
      next: () => { this.mensajeOk = 'Profesor eliminado.'; this.cargar(); },
      error: () => (this.errorMsg = 'Error eliminando profesor')
    });
  }

  toggleEspecialidad(id: number): void {
    const current: number[] = this.form.get('especialidadIds')?.value as number[] ?? [];
    const idx = current.indexOf(id);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(id);
    }
    this.form.get('especialidadIds')?.setValue([...current]);
  }

  isEspecialidadSelected(id: number): boolean {
    const current: number[] = this.form.get('especialidadIds')?.value as number[] ?? [];
    return current.includes(id);
  }
}
