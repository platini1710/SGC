import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrimestreService } from '../../../core/services/trimestre.service';
import { Trimestre } from '../../../core/models/trimestre.model';

@Component({
  selector: 'app-trimestres',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trimestres.component.html',
  styleUrl: './trimestres.component.scss'
})
export class TrimestresComponent implements OnInit {
  private trimestreService = inject(TrimestreService);

  trimestres: Trimestre[] = [];
  cargando = false;
  errorMsg = '';

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorMsg = '';
    this.trimestreService.getAll().subscribe({
      next: (t) => { this.trimestres = t; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando trimestres'; this.cargando = false; }
    });
  }

  alternar(t: Trimestre): void {
    this.errorMsg = '';
    const accion = t.estado === 'ABIERTO'
      ? this.trimestreService.cerrar(t.id)
      : this.trimestreService.abrir(t.id);
    accion.subscribe({
      next: () => this.cargar(),
      error: () => (this.errorMsg = 'Error actualizando el trimestre')
    });
  }
}
