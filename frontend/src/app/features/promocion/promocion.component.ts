import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatriculaService } from '../../core/services/matricula.service';
import { CursoService } from '../../core/services/curso.service';
import { AnioEscolarService } from '../../core/services/anio-escolar.service';
import { Curso } from '../../core/models/curso.model';
import {
  PromocionItemDTO,
  PromocionPreviewResponse,
  PromocionResultadoResponse,
  PromocionConfirmRequest,
  PromocionDecisionDTO
} from '../../core/models/promocion.model';

// Fila editable de la pantalla de promoción: combina el ítem propuesto con la
// decisión que el ADMIN puede ajustar antes de confirmar.
interface FilaPromocion {
  item: PromocionItemDTO;
  accion: string;              // PROMOVER | REPITENTE | EXCLUIR | EGRESA
  cursoDestinoId: number | null;
}

@Component({
  selector: 'app-promocion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './promocion.component.html',
  styleUrl: './promocion.component.scss'
})
export class PromocionComponent implements OnInit {
  private matriculaService = inject(MatriculaService);
  private cursoService = inject(CursoService);
  private anioEscolarService = inject(AnioEscolarService);

  cursos: Curso[] = [];
  anios: number[] = [];

  anioOrigen = this.anioEscolarService.anioActual;
  anioDestino = this.anioEscolarService.anioActual + 1;

  // Acciones que el ADMIN puede elegir por fila en la confirmación.
  readonly acciones = ['PROMOVER', 'REPITENTE', 'EXCLUIR', 'EGRESA'];

  preview: PromocionPreviewResponse | null = null;
  filas: FilaPromocion[] = [];
  resultado: PromocionResultadoResponse | null = null;

  cargando = false;
  confirmando = false;
  errorMsg = '';

  ngOnInit(): void {
    this.cursoService.getAll().subscribe({
      next: (c) => (this.cursos = c),
      error: () => (this.errorMsg = 'Error cargando cursos')
    });
    this.anioEscolarService.anios$.subscribe(a => (this.anios = a));
  }

  // Deriva la acción editable inicial desde la acción propuesta por el backend.
  private accionInicial(item: PromocionItemDTO): string {
    switch (item.accionPropuesta) {
      case 'PROMOVER': return 'PROMOVER';
      case 'EGRESA': return 'EGRESA';
      // SIN_CURSO_DESTINO u otros: por defecto repite hasta que el ADMIN decida.
      default: return 'REPITENTE';
    }
  }

  generarPreview(): void {
    this.errorMsg = '';
    this.resultado = null;
    if (this.anioOrigen == null || this.anioDestino == null) {
      this.errorMsg = 'Debe indicar año origen y año destino.';
      return;
    }
    if (this.anioOrigen === this.anioDestino) {
      this.errorMsg = 'El año destino debe ser distinto del año origen.';
      return;
    }
    this.cargando = true;
    this.matriculaService.promoverPreview({
      anioOrigen: this.anioOrigen,
      anioDestino: this.anioDestino
    }).subscribe({
      next: (p) => {
        this.preview = p;
        this.filas = p.items.map((item) => ({
          item,
          accion: this.accionInicial(item),
          cursoDestinoId: item.cursoDestinoId ?? null
        }));
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.errorMsg = err.error?.error ?? 'Error generando la previsualización.';
      }
    });
  }

  // El curso destino solo aplica a PROMOVER.
  requiereCurso(fila: FilaPromocion): boolean {
    return fila.accion === 'PROMOVER';
  }

  confirmar(): void {
    if (!this.preview) { return; }
    this.errorMsg = '';
    // Solo se envían decisiones que escriben matrícula o la excluyen explícitamente;
    // el backend tolera cualquier combinación y reporta incidencias.
    const decisiones: PromocionDecisionDTO[] = this.filas.map((f) => {
      const d: PromocionDecisionDTO = {
        estudianteId: f.item.estudianteId,
        accion: f.accion
      };
      if (f.accion === 'PROMOVER') {
        d.cursoDestinoId = f.cursoDestinoId;
      }
      return d;
    });

    // Validación local: PROMOVER requiere curso destino.
    const sinCurso = this.filas.some((f) => f.accion === 'PROMOVER' && f.cursoDestinoId == null);
    if (sinCurso) {
      this.errorMsg = 'Hay filas PROMOVER sin curso destino. Seleccione un curso o cambie la acción.';
      return;
    }

    const body: PromocionConfirmRequest = {
      anioOrigen: this.preview.anioOrigen,
      anioDestino: this.preview.anioDestino,
      decisiones
    };

    this.confirmando = true;
    this.matriculaService.promoverConfirmar(body).subscribe({
      next: (r) => {
        this.confirmando = false;
        this.resultado = r;
        this.preview = null;
        this.filas = [];
        // El año destino puede ser nuevo: refresca la lista de años disponibles.
        this.anioEscolarService.cargarAnios();
      },
      error: (err: HttpErrorResponse) => {
        this.confirmando = false;
        this.errorMsg = err.error?.error ?? 'Error confirmando la promoción.';
      }
    });
  }

  nombreCurso(id: number | null): string {
    if (id == null) { return '—'; }
    return this.cursos.find(c => c.id === id)?.nombre ?? '—';
  }
}
