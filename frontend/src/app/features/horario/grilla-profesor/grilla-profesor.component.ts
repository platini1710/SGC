import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HorarioService } from '../../../core/services/horario.service';
import { ProfesorService } from '../../../core/services/profesor.service';
import { Profesor } from '../../../core/models/profesor.model';
import { BloqueInfo, GrillaHorario, HorarioCelda } from '../../../core/models/horario.model';
import { BloqueCardComponent } from '../../../shared/bloque-card/bloque-card.component';

interface JornadaSeccion { jornada: string; label: string; bloques: BloqueInfo[]; }

const DIAS = [
  { num: 1, label: 'Lunes' },
  { num: 2, label: 'Martes' },
  { num: 3, label: 'Miércoles' },
  { num: 4, label: 'Jueves' },
  { num: 5, label: 'Viernes' },
];

@Component({
  selector: 'app-grilla-profesor',
  standalone: true,
  imports: [CommonModule, FormsModule, BloqueCardComponent],
  templateUrl: './grilla-profesor.component.html',
  styleUrl: './grilla-profesor.component.scss'
})
export class GrillaProfesorComponent implements OnInit {
  private horarioService = inject(HorarioService);
  private profesorService = inject(ProfesorService);

  profesores: Profesor[] = [];
  profesorSeleccionado: number | null = null;
  grilla: GrillaHorario | null = null;
  cargando = false;
  errorMsg = '';

  dias = DIAS;

  // Secciones por jornada ("Jornada JEC" / "Jornada Regular"), derivadas del payload.
  get secciones(): JornadaSeccion[] {
    const bloques = this.grilla?.bloques ?? [];
    const jec = bloques.filter((b) => b.jornada === 'JEC');
    const regular = bloques.filter((b) => b.jornada === 'REGULAR');
    const out: JornadaSeccion[] = [];
    if (jec.length) { out.push({ jornada: 'JEC', label: 'Jornada JEC', bloques: jec }); }
    if (regular.length) { out.push({ jornada: 'REGULAR', label: 'Jornada Regular', bloques: regular }); }
    return out;
  }

  ngOnInit(): void {
    this.profesorService.getAll().subscribe({
      next: (p) => (this.profesores = p),
      error: () => (this.errorMsg = 'Error cargando profesores')
    });
  }

  onProfesorChange(): void {
    if (this.profesorSeleccionado === null || this.profesorSeleccionado === undefined) { return; }
    const id = Number(this.profesorSeleccionado);
    if (!id) { return; }
    this.cargando = true;
    this.grilla = null;
    this.errorMsg = '';
    this.horarioService.getGrillaProfesor(id).subscribe({
      next: (g) => { this.grilla = g; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando horario'; this.cargando = false; }
    });
  }

  // Clave compuesta jornada-numero: el profesor cruza jornadas, así que numero no basta.
  getCelda(dia: number, claveRender: string): HorarioCelda | null {
    if (!this.grilla) { return null; }
    const diaData = this.grilla.grilla[String(dia)];
    if (!diaData) { return null; }
    return diaData[claveRender] ?? null;
  }
}
