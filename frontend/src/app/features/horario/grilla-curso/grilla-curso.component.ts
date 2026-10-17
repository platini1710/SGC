import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HorarioService } from '../../../core/services/horario.service';
import { CursoService } from '../../../core/services/curso.service';
import { Curso } from '../../../core/models/curso.model';
import { BloqueInfo, GrillaHorario, HorarioCelda } from '../../../core/models/horario.model';
import { BloqueCardComponent } from '../../../shared/bloque-card/bloque-card.component';
import { MoverBloqueModalComponent } from '../../../shared/mover-bloque-modal/mover-bloque-modal.component';

interface EditandoState { horarioId: number; dia: number; bloque: number; profesorId: number; }

const DIAS = [
  { num: 1, label: 'Lunes' },
  { num: 2, label: 'Martes' },
  { num: 3, label: 'Miércoles' },
  { num: 4, label: 'Jueves' },
  { num: 5, label: 'Viernes' },
];

@Component({
  selector: 'app-grilla-curso',
  standalone: true,
  imports: [CommonModule, FormsModule, BloqueCardComponent, MoverBloqueModalComponent],
  templateUrl: './grilla-curso.component.html',
  styleUrl: './grilla-curso.component.scss'
})
export class GrillaCursoComponent implements OnInit {
  private horarioService = inject(HorarioService);
  private cursoService = inject(CursoService);

  cursos: Curso[] = [];
  cursoSeleccionado: number | null = null;
  grilla: GrillaHorario | null = null;
  cargando = false;
  errorMsg = '';

  editando: EditandoState | null = null;

  dias = DIAS;

  // Las filas se derivan del payload (grilla.bloques), no de una constante fija.
  get bloques(): BloqueInfo[] {
    return this.grilla?.bloques ?? [];
  }

  ngOnInit(): void {
    this.cursoService.getAll().subscribe({
      next: (c) => (this.cursos = c),
      error: () => (this.errorMsg = 'Error cargando cursos')
    });
  }

  onCursoChange(): void {
    if (!this.cursoSeleccionado) { return; }
    this.cargarGrilla(this.cursoSeleccionado);
  }

  private cargarGrilla(cursoId: number): void {
    this.cargando = true;
    this.grilla = null;
    this.errorMsg = '';
    this.horarioService.getGrillaCurso(cursoId).subscribe({
      next: (g) => { this.grilla = g; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando horario'; this.cargando = false; }
    });
  }

  getCelda(dia: number, bloque: number): HorarioCelda | null {
    if (!this.grilla) { return null; }
    const diaData = this.grilla.grilla[String(dia)];
    if (!diaData) { return null; }
    return diaData[String(bloque)] ?? null;
  }

  abrirEditor(dia: number, bloque: number): void {
    const celda = this.getCelda(dia, bloque);
    if (!celda) { return; }
    this.editando = { horarioId: celda.horarioId, dia, bloque, profesorId: celda.profesorId };
  }

  onGuardado(): void {
    this.editando = null;
    if (this.cursoSeleccionado) { this.cargarGrilla(this.cursoSeleccionado); }
  }

  onCerrarModal(): void {
    this.editando = null;
  }
}
