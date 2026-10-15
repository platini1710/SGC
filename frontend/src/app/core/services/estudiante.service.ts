import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Estudiante, EstudianteRequest } from '../models/estudiante.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EstudianteService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(page = 0, size = 20, cursoId?: number): Observable<PageResponse<Estudiante>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (cursoId !== undefined) {
      params = params.set('cursoId', cursoId.toString());
    }
    return this.http.get<PageResponse<Estudiante>>(`${this.base}/api/estudiantes`, { params });
  }

  getById(id: number): Observable<Estudiante> {
    return this.http.get<Estudiante>(`${this.base}/api/estudiantes/${id}`);
  }

  create(req: EstudianteRequest): Observable<Estudiante> {
    return this.http.post<Estudiante>(`${this.base}/api/estudiantes`, req);
  }

  update(id: number, req: EstudianteRequest): Observable<Estudiante> {
    return this.http.put<Estudiante>(`${this.base}/api/estudiantes/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/estudiantes/${id}`);
  }
}
