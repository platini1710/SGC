import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HorarioService } from '../../../core/services/horario.service';
import { CursoService } from '../../../core/services/curso.service';
import { Curso } from '../../../core/models/curso.model';
import { GrillaHorario, HorarioCelda } from '../../../core/models/horario.model';
import { BloqueCardComponent } from '../../../shared/bloque-card/bloque-card.component';
import { MoverBloqueModalComponent } from '../../../shared/mover-bloque-modal/mover-bloque-modal.component';

interface BloqueInfo { numero: number; hora: string; esRecreo: boolean; }
interface EditandoState { horarioId: number; dia: number; bloque: number; profesorId: number; }

const BLOQUES: BloqueInfo[] = [
  { numero: 1, hora: '08:00–08:45', esRecreo: false },
  { numero: 2, hora: '08:45–09:30', esRecreo: false },
  { numero: 3, hora: '09:30–10:15', esRecreo: false },
  { numero: 4, hora: '10:15–10:45', esRecreo: true },
  { numero: 5, hora: '10:45–11:30', esRecreo: false },
  { numero: 6, hora: '11:30–12:15', esRecreo: false },
  { numero: 7, hora: '12:15–13:00', esRecreo: false },
  { numero: 8, hora: '13:00–13:45', esRecreo: false },
];

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

  bloques = BLOQUES;
  dias = DIAS;

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
