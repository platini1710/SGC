import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Profesor, ProfesorRequest } from '../models/profesor.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProfesorService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Profesor[]> {
    return this.http.get<Profesor[]>(`${this.base}/api/profesores`);
  }

  getById(id: number): Observable<Profesor> {
    return this.http.get<Profesor>(`${this.base}/api/profesores/${id}`);
  }

  create(req: ProfesorRequest): Observable<Profesor> {
    return this.http.post<Profesor>(`${this.base}/api/profesores`, req);
  }

  update(id: number, req: ProfesorRequest): Observable<Profesor> {
    return this.http.put<Profesor>(`${this.base}/api/profesores/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/profesores/${id}`);
  }
}
