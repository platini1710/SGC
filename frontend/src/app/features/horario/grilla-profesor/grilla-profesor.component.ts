import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HorarioService } from '../../../core/services/horario.service';
import { ProfesorService } from '../../../core/services/profesor.service';
import { Profesor } from '../../../core/models/profesor.model';
import { GrillaHorario, HorarioCelda } from '../../../core/models/horario.model';
import { BloqueCardComponent } from '../../../shared/bloque-card/bloque-card.component';

interface BloqueInfo { numero: number; hora: string; esRecreo: boolean; }

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

  bloques = BLOQUES;
  dias = DIAS;

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

  getCelda(dia: number, bloque: number): HorarioCelda | null {
    if (!this.grilla) { return null; }
    const diaData = this.grilla.grilla[String(dia)];
    if (!diaData) { return null; }
    return diaData[String(bloque)] ?? null;
  }
}
