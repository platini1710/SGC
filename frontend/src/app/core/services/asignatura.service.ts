import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asignatura, AsignaturaRequest } from '../models/asignatura.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AsignaturaService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Asignatura[]> {
    return this.http.get<Asignatura[]>(`${this.base}/api/asignaturas`);
  }

  getById(id: number): Observable<Asignatura> {
    return this.http.get<Asignatura>(`${this.base}/api/asignaturas/${id}`);
  }

  create(req: AsignaturaRequest): Observable<Asignatura> {
    return this.http.post<Asignatura>(`${this.base}/api/asignaturas`, req);
  }

  update(id: number, req: AsignaturaRequest): Observable<Asignatura> {
    return this.http.put<Asignatura>(`${this.base}/api/asignaturas/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/asignaturas/${id}`);
  }
}
