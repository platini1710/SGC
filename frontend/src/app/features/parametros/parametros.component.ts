import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParametroService, Parametros } from '../../core/services/parametro.service';

@Component({
  selector: 'app-parametros',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parametros.component.html',
  styleUrl: './parametros.component.scss'
})
export class ParametrosComponent implements OnInit {
  private parametroService = inject(ParametroService);

  // Switch de aproximación: true = 'S' (aproxima), false = 'N' (trunca).
  aproxima = false;
  timeoutInactividad = 60;
  anioEscolarActivo = new Date().getFullYear();

  // Jornada Escolar Completa (JEC).
  tipoJornada = 'JEC';
  duracionRecreo = 15;
  horaInicioJornada = '08:00';
  duracionBloqueMin = 90;
  duracionAlmuerzo = 75;

  cargando = false;
  guardando = false;
  errorMsg = '';
  mensajeOk = '';

  ngOnInit(): void {
    this.cargando = true;
    this.parametroService.get().subscribe({
      next: (p) => {
        this.aproxima = (p.aproxNota ?? 'N').trim().toUpperCase() === 'S';
        this.timeoutInactividad = p.timeoutInactividad ?? 60;
        this.anioEscolarActivo = p.anioEscolarActivo ?? new Date().getFullYear();
        this.tipoJornada = p.tipoJornada ?? 'JEC';
        this.duracionRecreo = p.duracionRecreo ?? 15;
        this.horaInicioJornada = p.horaInicioJornada ?? '08:00';
        this.duracionBloqueMin = p.duracionBloqueMin ?? 90;
        this.duracionAlmuerzo = p.duracionAlmuerzo ?? 75;
        this.cargando = false;
      },
      error: () => { this.errorMsg = 'Error cargando los parámetros'; this.cargando = false; }
    });
  }

  guardar(): void {
    this.guardando = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    const body: Parametros = {
      aproxNota: this.aproxima ? 'S' : 'N',
      timeoutInactividad: this.timeoutInactividad,
      anioEscolarActivo: this.anioEscolarActivo,
      tipoJornada: this.tipoJornada,
      duracionRecreo: this.duracionRecreo,
      horaInicioJornada: this.horaInicioJornada,
      duracionBloqueMin: this.duracionBloqueMin,
      duracionAlmuerzo: this.duracionAlmuerzo
    };
    this.parametroService.actualizar(body).subscribe({
      next: () => { this.guardando = false; this.mensajeOk = 'Parámetros guardados correctamente.'; },
      error: () => { this.guardando = false; this.errorMsg = 'Error guardando los parámetros.'; }
    });
  }
}
