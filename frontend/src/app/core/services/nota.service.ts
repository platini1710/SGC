import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GrillaNotas, NotaRequest, NotaResponse, ConsolidadoAnual, AsignacionDocente } from '../models/nota.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotaService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getGrilla(cursoId: number, asignaturaId: number, trimestreId: number): Observable<GrillaNotas> {
    const params = new HttpParams()
      .set('cursoId', cursoId.toString())
      .set('asignaturaId', asignaturaId.toString())
      .set('trimestreId', trimestreId.toString());
    return this.http.get<GrillaNotas>(`${this.base}/api/notas/grilla`, { params });
  }

  guardar(req: NotaRequest): Observable<NotaResponse> {
    return this.http.post<NotaResponse>(`${this.base}/api/notas`, req);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/notas/${id}`);
  }

  // Obtiene el parámetro de aproximación: { aproxNota: 'S' | 'N' | ... }
  getParametros(): Observable<{ aproxNota: string }> {
    return this.http.get<{ aproxNota: string }>(`${this.base}/api/parametros`);
  }

  // Asignaciones del docente autenticado (curso + asignatura).
  getMisAsignaciones(): Observable<AsignacionDocente[]> {
    return this.http.get<AsignacionDocente[]>(`${this.base}/api/notas/mis-asignaciones`);
  }

  getConsolidado(cursoId: number, asignaturaId: number, anio: number): Observable<ConsolidadoAnual> {
    const params = new HttpParams()
      .set('cursoId', cursoId.toString())
      .set('asignaturaId', asignaturaId.toString())
      .set('anio', anio.toString());
    return this.http.get<ConsolidadoAnual>(`${this.base}/api/notas/consolidado`, { params });
  }
}
