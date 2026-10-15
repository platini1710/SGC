import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EstudianteService } from '../../core/services/estudiante.service';
import { CursoService } from '../../core/services/curso.service';
import { ApoderadoService } from '../../core/services/apoderado.service';
import { ReporteService } from '../../core/services/reporte.service';
import { Estudiante, EstudianteRequest } from '../../core/models/estudiante.model';
import { PageResponse } from '../../core/models/page.model';
import { Curso } from '../../core/models/curso.model';
import { Apoderado } from '../../core/models/apoderado.model';

@Component({
  selector: 'app-estudiantes-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './estudiantes-list.component.html',
  styleUrl: './estudiantes-list.component.scss'
})
export class EstudiantesListComponent implements OnInit {
  private estudianteService = inject(EstudianteService);
  private cursoService = inject(CursoService);
  private apoderadoService = inject(ApoderadoService);
  private reporteService = inject(ReporteService);
  private fb = inject(FormBuilder);

  pageData: PageResponse<Estudiante> | null = null;
  cursos: Curso[] = [];
  apoderados: Apoderado[] = [];
  page = 0;
  size = 20;
  cursoFiltro: number | null = null;
  form!: FormGroup;
  mostrarForm = false;
  editandoId: number | null = null;
  cargando = false;
  errorMsg = '';
  mensajeOk = '';
  fotoPreview: string | null = null;

  // Tamaño máximo permitido para la foto (~2 MB).
  private readonly maxFotoBytes = 2 * 1024 * 1024;

  ngOnInit(): void {
    this.initForm();
    this.cursoService.getAll().subscribe(c => (this.cursos = c));
    this.apoderadoService.getAll().subscribe(a => (this.apoderados = a));
    this.cargar();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      rut: [''],
      fechaNacimiento: [''],
      cursoId: [null as number | null],
      apoderadoId: [null as number | null],
      // Identificación
      lugarNacimiento: [''],
      nacionalidad: [''],
      sexo: [''],
      comuna: [''],
      telefono: [''],
      domicilio: [''],
      foto: [''],
      // Antecedentes familiares
      padreNombre: [''],
      padreRut: [''],
      padreOcupacion: [''],
      madreNombre: [''],
      madreRut: [''],
      madreOcupacion: [''],
      apoderadoRelacion: [''],
      // Información médica
      prevision: [''],
      alergias: [''],
      condicionMedica: [''],
      contactoEmergencia: [''],
      fonoEmergencia: ['']
    });
  }

  cargar(): void {
    this.cargando = true;
    this.estudianteService.getAll(this.page, this.size, this.cursoFiltro ?? undefined).subscribe({
      next: (p) => { this.pageData = p; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando estudiantes'; this.cargando = false; }
    });
  }

  onFiltroCurso(value: string): void {
    this.cursoFiltro = value === '' ? null : Number(value);
    this.page = 0;
    this.cargar();
  }

  anterior(): void {
    if (this.pageData && this.page > 0) {
      this.page--;
      this.cargar();
    }
  }

  siguiente(): void {
    if (this.pageData && this.page < this.pageData.totalPages - 1) {
      this.page++;
      this.cargar();
    }
  }

  nombreCurso(id: number): string {
    return this.cursos.find(c => c.id === id)?.nombre ?? '—';
  }

  abrirForm(e?: Estudiante): void {
    this.mostrarForm = true;
    this.mensajeOk = '';
    this.errorMsg = '';
    if (e) {
      this.editandoId = e.id;
      this.form.setValue({
        nombre: e.nombre,
        apellido: e.apellido,
        rut: e.rut ?? '',
        fechaNacimiento: e.fechaNacimiento ?? '',
        cursoId: e.cursoId ?? null,
        apoderadoId: e.apoderadoId ?? null,
        lugarNacimiento: e.lugarNacimiento ?? '',
        nacionalidad: e.nacionalidad ?? '',
        sexo: e.sexo ?? '',
        comuna: e.comuna ?? '',
        telefono: e.telefono ?? '',
        domicilio: e.domicilio ?? '',
        foto: e.foto ?? '',
        padreNombre: e.padreNombre ?? '',
        padreRut: e.padreRut ?? '',
        padreOcupacion: e.padreOcupacion ?? '',
        madreNombre: e.madreNombre ?? '',
        madreRut: e.madreRut ?? '',
        madreOcupacion: e.madreOcupacion ?? '',
        apoderadoRelacion: e.apoderadoRelacion ?? '',
        prevision: e.prevision ?? '',
        alergias: e.alergias ?? '',
        condicionMedica: e.condicionMedica ?? '',
        contactoEmergencia: e.contactoEmergencia ?? '',
        fonoEmergencia: e.fonoEmergencia ?? ''
      });
      this.fotoPreview = e.foto ?? null;
    } else {
      this.editandoId = null;
      this.form.reset({
        nombre: '', apellido: '', rut: '', fechaNacimiento: '', cursoId: null, apoderadoId: null,
        lugarNacimiento: '', nacionalidad: '', sexo: '', comuna: '', telefono: '', domicilio: '', foto: '',
        padreNombre: '', padreRut: '', padreOcupacion: '', madreNombre: '', madreRut: '', madreOcupacion: '',
        apoderadoRelacion: '', prevision: '', alergias: '', condicionMedica: '', contactoEmergencia: '', fonoEmergencia: ''
      });
      this.fotoPreview = null;
    }
  }

  cancelar(): void {
    this.mostrarForm = false;
    this.editandoId = null;
    this.fotoPreview = null;
    this.form.reset();
  }

  guardar(): void {
    if (this.form.invalid) { return; }
    const val = this.form.value as {
      nombre: string; apellido: string; rut: string; fechaNacimiento: string;
      cursoId: number | null; apoderadoId: number | null;
      lugarNacimiento: string; nacionalidad: string; sexo: string; comuna: string;
      telefono: string; domicilio: string; foto: string;
      padreNombre: string; padreRut: string; padreOcupacion: string;
      madreNombre: string; madreRut: string; madreOcupacion: string; apoderadoRelacion: string;
      prevision: string; alergias: string; condicionMedica: string;
      contactoEmergencia: string; fonoEmergencia: string;
    };
    const req: EstudianteRequest = {
      nombre: val.nombre,
      apellido: val.apellido,
      rut: val.rut,
      fechaNacimiento: val.fechaNacimiento || undefined,
      cursoId: val.cursoId,
      apoderadoId: val.apoderadoId,
      lugarNacimiento: val.lugarNacimiento || undefined,
      nacionalidad: val.nacionalidad || undefined,
      sexo: val.sexo || undefined,
      comuna: val.comuna || undefined,
      telefono: val.telefono || undefined,
      domicilio: val.domicilio || undefined,
      foto: val.foto || undefined,
      padreNombre: val.padreNombre || undefined,
      padreRut: val.padreRut || undefined,
      padreOcupacion: val.padreOcupacion || undefined,
      madreNombre: val.madreNombre || undefined,
      madreRut: val.madreRut || undefined,
      madreOcupacion: val.madreOcupacion || undefined,
      apoderadoRelacion: val.apoderadoRelacion || undefined,
      prevision: val.prevision || undefined,
      alergias: val.alergias || undefined,
      condicionMedica: val.condicionMedica || undefined,
      contactoEmergencia: val.contactoEmergencia || undefined,
      fonoEmergencia: val.fonoEmergencia || undefined
    };
    if (this.editandoId !== null) {
      this.estudianteService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = 'Estudiante actualizado.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error actualizando estudiante')
      });
    } else {
      this.estudianteService.create(req).subscribe({
        next: () => { this.mensajeOk = 'Estudiante creado.'; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error creando estudiante')
      });
    }
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Eliminar este estudiante?')) { return; }
    this.estudianteService.delete(id).subscribe({
      next: () => { this.mensajeOk = 'Estudiante eliminado.'; this.cargar(); },
      error: () => (this.errorMsg = 'Error eliminando estudiante')
    });
  }

  // Lee la imagen seleccionada, valida tipo y tamaño (~2 MB) y la guarda como data URI.
  onFotoSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) { return; }
    if (!file.type.startsWith('image/')) {
      this.errorMsg = 'El archivo seleccionado no es una imagen.';
      input.value = '';
      return;
    }
    if (file.size > this.maxFotoBytes) {
      this.errorMsg = 'La imagen supera el tamaño máximo permitido (2 MB).';
      input.value = '';
      return;
    }
    this.errorMsg = '';
    const reader = new FileReader();
    reader.onload = () => {
      const dataUri = reader.result as string;
      this.form.patchValue({ foto: dataUri });
      this.fotoPreview = dataUri;
    };
    reader.readAsDataURL(file);
  }

  quitarFoto(): void {
    this.form.patchValue({ foto: '' });
    this.fotoPreview = null;
  }

  // Descarga la Ficha de Matrícula del alumno con el patrón blob -> <a> -> click -> revoke.
  descargarFicha(id: number): void {
    this.reporteService.descargarFicha(id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ficha-alumno-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => (this.errorMsg = 'Error generando la ficha del alumno.')
    });
  }
}
