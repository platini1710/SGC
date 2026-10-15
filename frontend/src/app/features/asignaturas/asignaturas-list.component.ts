import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AsignaturaService } from '../../core/services/asignatura.service';
import { Asignatura, AsignaturaRequest } from '../../core/models/asignatura.model';

@Component({
  selector: 'app-asignaturas-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './asignaturas-list.component.html',
  styleUrl: './asignaturas-list.component.scss'
})
export class AsignaturasListComponent implements OnInit {
  private asignaturaService = inject(AsignaturaService);
  private fb = inject(FormBuilder);

  asignaturas: Asignatura[] = [];
  form!: FormGroup;
  mostrarForm = false;
  editandoId: number | null = null;
  cargando = false;
  errorMsg = '';
  mensajeOk = '';

  ngOnInit(): void {
    this.initForm();
    this.cargar();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      codigo: [''],
      descripcion: ['']
    });
  }

  cargar(): void {
    this.cargando = true;
    this.asignaturaService.getAll().subscribe({
      next: (a) => { this.asignaturas = a; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando asignaturas'; this.cargando = false; }
    });
  }

  abrirForm(a?: Asignatura): void {
    this.mostrarForm = true;
    this.mensajeOk = '';
    this.errorMsg = '';
    if (a) {
      this.editandoId = a.id;
      this.form.setValue({
        nombre: a.nombre,
        codigo: a.codigo ?? '',
        descripcion: a.descripcion ?? ''
      });
    } else {
      this.editandoId = null;
      this.form.reset({ nombre: '', codigo: '', descripcion: '' });
    }
  }

  cancelar(): void {
    this.mostrarForm = false;
    this.editandoId = null;
    this.form.reset();
  }

  guardar(): void {
    if (this.form.invalid) { return; }
    const val = this.form.value as { nombre: string; codigo: string; descripcion: string };
    const req: AsignaturaRequest = { nombre: val.nombre, codigo: val.codigo, descripcion: val.descripcion };
    if (this.editandoId !== null) {
      this.asignaturaService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = 'Asignatura actualizada.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error actualizando asignatura')
      });
    } else {
      this.asignaturaService.create(req).subscribe({
        next: () => { this.mensajeOk = 'Asignatura creada.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error creando asignatura')
      });
    }
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Eliminar esta asignatura?')) { return; }
    this.asignaturaService.delete(id).subscribe({
      next: () => { this.mensajeOk = 'Asignatura eliminada.'; this.cargar(); },
      error: () => (this.errorMsg = 'Error eliminando asignatura')
    });
  }
}
