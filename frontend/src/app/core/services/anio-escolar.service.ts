import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ParametroService } from './parametro.service';
import { TrimestreService } from './trimestre.service';

// Store en memoria del año escolar seleccionado. El valor por defecto es el
// anioEscolarActivo leído de GET /api/parametros; también mantiene la lista de
// años disponibles (GET /api/trimestres/anios) para poblar los selectores.
@Injectable({ providedIn: 'root' })
export class AnioEscolarService {
  private parametroService = inject(ParametroService);
  private trimestreService = inject(TrimestreService);

  private readonly anioSubject = new BehaviorSubject<number>(new Date().getFullYear());
  private readonly aniosSubject = new BehaviorSubject<number[]>([]);

  // Año seleccionado (observable) y lista de años disponibles (observable).
  readonly anio$: Observable<number> = this.anioSubject.asObservable();
  readonly anios$: Observable<number[]> = this.aniosSubject.asObservable();

  constructor() {
    // Inicializa el año seleccionado desde el parámetro global.
    this.parametroService.get().subscribe({
      next: (p) => {
        if (p.anioEscolarActivo != null) {
          this.anioSubject.next(p.anioEscolarActivo);
        }
      },
      error: () => { /* mantiene el año por defecto (año actual) */ }
    });
    // Carga los años disponibles para los selectores.
    this.cargarAnios();
  }

  get anioActual(): number {
    return this.anioSubject.value;
  }

  get anios(): number[] {
    return this.aniosSubject.value;
  }

  setAnio(anio: number): void {
    this.anioSubject.next(anio);
  }

  // (Re)carga la lista de años con datos; asegura que el año actual esté incluido.
  cargarAnios(): void {
    this.trimestreService.getAnios().subscribe({
      next: (anios) => {
        const lista = [...anios];
        if (!lista.includes(this.anioActual)) {
          lista.push(this.anioActual);
          lista.sort((a, b) => b - a);
        }
        this.aniosSubject.next(lista);
      },
      error: () => this.aniosSubject.next([this.anioActual])
    });
  }
}
