import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApoderadoService } from '../../core/services/apoderado.service';
import { Apoderado, ApoderadoRequest } from '../../core/models/apoderado.model';

const CORREO_DEFAULT = 'aespinoza3010@gmail.com';

@Component({
  selector: 'app-apoderados-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './apoderados-list.component.html',
  styleUrl: './apoderados-list.component.scss'
})
export class ApoderadosListComponent implements OnInit {
  private apoderadoService = inject(ApoderadoService);
  private fb = inject(FormBuilder);

  apoderados: Apoderado[] = [];
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
      rut: ['', Validators.required],
      nombre: ['', Validators.required],
      apellidoPaterno: ['', Validators.required],
      region: [''],
      comuna: [''],
      direccion: [''],
      telefonoCelular: [''],
      correo: [CORREO_DEFAULT, Validators.email]
    });
  }

  cargar(): void {
    this.cargando = true;
    this.apoderadoService.getAll().subscribe({
      next: (a) => { this.apoderados = a; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando apoderados'; this.cargando = false; }
    });
  }

  abrirForm(a?: Apoderado): void {
    this.mostrarForm = true;
    this.mensajeOk = '';
    this.errorMsg = '';
    if (a) {
      this.editandoId = a.id;
      this.form.setValue({
        rut: a.rut,
        nombre: a.nombre,
        apellidoPaterno: a.apellidoPaterno,
        region: a.region ?? '',
        comuna: a.comuna ?? '',
        direccion: a.direccion ?? '',
        telefonoCelular: a.telefonoCelular ?? '',
        correo: a.correo ?? ''
      });
    } else {
      this.editandoId = null;
      this.form.reset({
        rut: '',
        nombre: '',
        apellidoPaterno: '',
        region: '',
        comuna: '',
        direccion: '',
        telefonoCelular: '',
        correo: CORREO_DEFAULT
      });
    }
  }

  cancelar(): void {
    this.mostrarForm = false;
    this.editandoId = null;
    this.form.reset();
  }

  guardar(): void {
    if (this.form.invalid) { return; }
    const val = this.form.value as {
      rut: string; nombre: string; apellidoPaterno: string; region: string;
      comuna: string; direccion: string; telefonoCelular: string; correo: string;
    };
    const req: ApoderadoRequest = {
      rut: val.rut,
      nombre: val.nombre,
      apellidoPaterno: val.apellidoPaterno,
      region: val.region,
      comuna: val.comuna,
      direccion: val.direccion,
      telefonoCelular: val.telefonoCelular,
      correo: val.correo
    };
    if (this.editandoId !== null) {
      this.apoderadoService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = 'Apoderado actualizado.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error actualizando apoderado')
      });
    } else {
      this.apoderadoService.create(req).subscribe({
        next: () => { this.mensajeOk = 'Apoderado creado.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error creando apoderado')
      });
    }
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Eliminar este apoderado?')) { return; }
    this.apoderadoService.delete(id).subscribe({
      next: () => { this.mensajeOk = 'Apoderado eliminado.'; this.cargar(); },
      error: () => (this.errorMsg = 'Error eliminando apoderado')
    });
  }
}
