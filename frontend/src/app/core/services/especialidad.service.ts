import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Especialidad, EspecialidadRequest } from '../models/especialidad.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EspecialidadService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Especialidad[]> {
    return this.http.get<Especialidad[]>(`${this.base}/api/especialidades`);
  }

  getById(id: number): Observable<Especialidad> {
    return this.http.get<Especialidad>(`${this.base}/api/especialidades/${id}`);
  }

  create(req: EspecialidadRequest): Observable<Especialidad> {
    return this.http.post<Especialidad>(`${this.base}/api/especialidades`, req);
  }

  update(id: number, req: EspecialidadRequest): Observable<Especialidad> {
    return this.http.put<Especialidad>(`${this.base}/api/especialidades/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/especialidades/${id}`);
  }
}
