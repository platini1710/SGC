import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HorarioService } from '../../../core/services/horario.service';

@Component({
  selector: 'app-horario-admin',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './horario-admin.component.html',
  styleUrl: './horario-admin.component.scss'
})
export class HorarioAdminComponent {
  private horarioService = inject(HorarioService);

  generando = false;
  borrando = false;
  mensajeGenerar = '';
  mensajeBorrar = '';
  errorGenerar = '';
  errorBorrar = '';

  generarHorario(): void {
    this.generando = true;
    this.mensajeGenerar = '';
    this.errorGenerar = '';
    this.horarioService.generarHorario().subscribe({
      next: () => {
        this.generando = false;
        this.mensajeGenerar = '✅ Horario generado exitosamente.';
      },
      error: (err) => {
        this.generando = false;
        this.errorGenerar = `Error al generar horario: ${err.message ?? 'Error desconocido'}`;
      }
    });
  }

  borrarHorario(): void {
    const confirm = window.confirm('¿Está seguro que desea borrar todos los horarios? Esta acción no se puede deshacer.');
    if (!confirm) { return; }
    this.borrando = true;
    this.mensajeBorrar = '';
    this.errorBorrar = '';
    this.horarioService.borrarHorario().subscribe({
      next: () => {
        this.borrando = false;
        this.mensajeBorrar = '✅ Horarios borrados exitosamente.';
      },
      error: (err) => {
        this.borrando = false;
        this.errorBorrar = `Error al borrar horario: ${err.message ?? 'Error desconocido'}`;
      }
    });
  }
}
