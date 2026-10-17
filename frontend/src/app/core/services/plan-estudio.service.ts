import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanEstudio, PlanEstudioDetalle, PlanEstudioRequest } from '../models/plan-estudio.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PlanEstudioService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<PlanEstudio[]> {
    return this.http.get<PlanEstudio[]>(`${this.base}/api/planes-estudio`);
  }

  getById(id: number): Observable<PlanEstudioDetalle> {
    return this.http.get<PlanEstudioDetalle>(`${this.base}/api/planes-estudio/${id}`);
  }

  create(req: PlanEstudioRequest): Observable<PlanEstudio> {
    return this.http.post<PlanEstudio>(`${this.base}/api/planes-estudio`, req);
  }

  update(id: number, req: PlanEstudioRequest): Observable<PlanEstudio> {
    return this.http.put<PlanEstudio>(`${this.base}/api/planes-estudio/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/planes-estudio/${id}`);
  }

  addAsignatura(planId: number, asignaturaId: number): Observable<PlanEstudioDetalle> {
    return this.http.post<PlanEstudioDetalle>(
      `${this.base}/api/planes-estudio/${planId}/asignaturas`, { asignaturaId });
  }

  removeAsignatura(planId: number, asignaturaId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/api/planes-estudio/${planId}/asignaturas/${asignaturaId}`);
  }
}
