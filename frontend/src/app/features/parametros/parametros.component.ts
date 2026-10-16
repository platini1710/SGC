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
      anioEscolarActivo: this.anioEscolarActivo
    };
    this.parametroService.actualizar(body).subscribe({
      next: () => { this.guardando = false; this.mensajeOk = 'Parámetros guardados correctamente.'; },
      error: () => { this.guardando = false; this.errorMsg = 'Error guardando los parámetros.'; }
    });
  }
}
