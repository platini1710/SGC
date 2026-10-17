import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Parametros {
  aproxNota: string;              // 'S' aproxima, otro trunca
  timeoutInactividad: number;     // segundos
  anioEscolarActivo: number;      // año escolar en curso
  // Jornada Escolar Completa (JEC).
  tipoJornada: string;            // "JEC" por ahora
  duracionRecreo: number;         // minutos
  horaInicioJornada: string;      // "HH:mm"
  duracionBloqueMin: number;      // minutos (90 = 2 horas pedagógicas)
  duracionAlmuerzo: number;       // minutos
}

@Injectable({ providedIn: 'root' })
export class ParametroService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  get(): Observable<Parametros> {
    return this.http.get<Parametros>(`${this.base}/api/parametros`);
  }

  actualizar(p: Parametros): Observable<Parametros> {
    return this.http.put<Parametros>(`${this.base}/api/parametros`, p);
  }
}
