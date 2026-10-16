import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EstudianteService } from '../../core/services/estudiante.service';
import { CursoService } from '../../core/services/curso.service';
import { ApoderadoService } from '../../core/services/apoderado.service';
import { ReporteService } from '../../core/services/reporte.service';
import { AnioEscolarService } from '../../core/services/anio-escolar.service';
import { Estudiante, EstudianteRequest } from '../../core/models/estudiante.model';
import { PageResponse } from '../../core/models/page.model';
import { Curso } from '../../core/models/curso.model';
import { Apoderado } from '../../core/models/apoderado.model';

@Component({
  selector: 'app-estudiantes-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './estudiantes-list.component.html',
  styleUrl: './estudiantes-list.component.scss'
})
export class EstudiantesListComponent implements OnInit {
  private estudianteService = inject(EstudianteService);
  private cursoService = inject(CursoService);
  private apoderadoService = inject(ApoderadoService);
  private reporteService = inject(ReporteService);
  private anioEscolarService = inject(AnioEscolarService);
  private fb = inject(FormBuilder);

  pageData: PageResponse<Estudiante> | null = null;
  cursos: Curso[] = [];
  apoderados: Apoderado[] = [];
  // Años disponibles y año seleccionado (por defecto el año escolar activo).
  anios: number[] = [];
  anioFiltro = this.anioEscolarService.anioActual;
  page = 0;
  size = 20;
  cursoFiltro: number | null = null;
  // Filtros de búsqueda en vivo (sobre la página cargada).
  filtroRut = '';
  filtroApellido = '';
  // Alterna entre el listado de activos y el de inactivos.
  verInactivos = false;
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
    this.anioEscolarService.anios$.subscribe(a => (this.anios = a));
    this.anioEscolarService.anio$.subscribe(a => (this.anioFiltro = a));
    this.cargar();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      apellidoPaterno: ['', Validators.required],
      apellidoMaterno: [''],
      rut: [''],
      fechaNacimiento: [''],
      cursoId: [null as number | null],
      // El apoderado es obligatorio: debe existir previamente en el mantenedor de Apoderados.
      apoderadoId: [null as number | null, Validators.required],
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
    // El año solo aplica cuando se filtra por curso (listado por matrícula del año).
    const anio = this.cursoFiltro != null ? this.anioFiltro : undefined;
    const obs = this.verInactivos
      ? this.estudianteService.getInactivos(this.page, this.size)
      : this.estudianteService.getAll(this.page, this.size, this.cursoFiltro ?? undefined, anio);
    obs.subscribe({
      next: (p) => { this.pageData = p; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando estudiantes'; this.cargando = false; }
    });
  }

  verActivos(): void {
    this.verInactivos = false;
    this.page = 0;
    this.mostrarForm = false;
    this.mensajeOk = '';
    this.errorMsg = '';
    this.cargar();
  }

  verInactivosLista(): void {
    this.verInactivos = true;
    this.page = 0;
    this.mostrarForm = false;
    this.limpiarFiltros();
    this.mensajeOk = '';
    this.errorMsg = '';
    this.cargar();
  }

  reactivar(id: number): void {
    const e = (this.pageData?.content ?? []).find(x => x.id === id);
    const descripcion = e
      ? `${e.nombre} ${e.apellidoPaterno}${e.apellidoMaterno ? ' ' + e.apellidoMaterno : ''} con RUT ${e.rut || '—'}`
      : '';
    if (!window.confirm('¿Reactivar este estudiante? Volverá a aparecer en el listado de activos.')) { return; }
    this.estudianteService.reactivar(id).subscribe({
      next: () => { this.mensajeOk = `Estudiante ${descripcion} reactivado.`; this.cargar(); },
      error: () => (this.errorMsg = 'Error al reactivar el estudiante')
    });
  }

  onFiltroCurso(value: string): void {
    this.cursoFiltro = value === '' ? null : Number(value);
    this.page = 0;
    this.cargar();
  }

  onFiltroAnio(value: string): void {
    this.anioFiltro = Number(value);
    this.anioEscolarService.setAnio(this.anioFiltro);
    this.page = 0;
    this.cargar();
  }

  // Lista mostrada: aplica los filtros de RUT y apellido sobre la página cargada.
  get estudiantesFiltrados(): Estudiante[] {
    const items = this.pageData?.content ?? [];
    const rut = this.filtroRut.trim().toLowerCase();
    const apellido = this.filtroApellido.trim().toLowerCase();
    if (!rut && !apellido) { return items; }
    return items.filter((e) => {
      const okRut = !rut || (e.rut ?? '').toLowerCase().includes(rut);
      const okApe = !apellido
        || (e.apellidoPaterno ?? '').toLowerCase().includes(apellido)
        || (e.apellidoMaterno ?? '').toLowerCase().includes(apellido);
      return okRut && okApe;
    });
  }

  limpiarFiltros(): void {
    this.filtroRut = '';
    this.filtroApellido = '';
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
        apellidoPaterno: e.apellidoPaterno,
        apellidoMaterno: e.apellidoMaterno ?? '',
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
      // RUT inmutable en edición.
      this.form.get('rut')?.disable();
    } else {
      this.editandoId = null;
      this.form.reset({
        nombre: '', apellidoPaterno: '', apellidoMaterno: '', rut: '', fechaNacimiento: '', cursoId: null, apoderadoId: null,
        lugarNacimiento: '', nacionalidad: '', sexo: '', comuna: '', telefono: '', domicilio: '', foto: '',
        padreNombre: '', padreRut: '', padreOcupacion: '', madreNombre: '', madreRut: '', madreOcupacion: '',
        apoderadoRelacion: '', prevision: '', alergias: '', condicionMedica: '', contactoEmergencia: '', fonoEmergencia: ''
      });
      this.fotoPreview = null;
      // RUT editable al crear.
      this.form.get('rut')?.enable();
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
    // getRawValue incluye el control RUT aunque esté deshabilitado en edición.
    const val = this.form.getRawValue() as {
      nombre: string; apellidoPaterno: string; apellidoMaterno: string; rut: string; fechaNacimiento: string;
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
      apellidoPaterno: val.apellidoPaterno,
      apellidoMaterno: val.apellidoMaterno || undefined,
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
    // Nombre completo + RUT para el mensaje de confirmación.
    const descripcion = `${val.nombre} ${val.apellidoPaterno}${val.apellidoMaterno ? ' ' + val.apellidoMaterno : ''}, RUT ${val.rut || '—'}`;
    if (this.editandoId !== null) {
      this.estudianteService.update(this.editandoId, req).subscribe({
        next: () => { this.mensajeOk = `Estudiante ${descripcion} actualizado.`; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error actualizando estudiante')
      });
    } else {
      this.estudianteService.create(req).subscribe({
        next: () => { this.mensajeOk = `Estudiante ${descripcion} creado.`; this.cancelar(); this.cargar(); },
        error: () => (this.errorMsg = 'Error creando estudiante')
      });
    }
  }

  eliminar(id: number): void {
    if (!window.confirm('¿Marcar este estudiante como inactivo? Dejará de aparecer en el listado.')) { return; }
    // Busca el estudiante en la lista cargada para armar el mensaje con nombre y RUT.
    const e = (this.pageData?.content ?? []).find(x => x.id === id);
    const descripcion = e
      ? `${e.nombre} ${e.apellidoPaterno}${e.apellidoMaterno ? ' ' + e.apellidoMaterno : ''} con RUT ${e.rut || '—'}`
      : '';
    this.estudianteService.delete(id).subscribe({
      next: () => { this.mensajeOk = `Estudiante ${descripcion} marcado como inactivo.`; this.cargar(); },
      error: () => (this.errorMsg = 'Error al inactivar el estudiante')
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
