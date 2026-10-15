import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Apoderado, ApoderadoRequest } from '../models/apoderado.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApoderadoService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Apoderado[]> {
    return this.http.get<Apoderado[]>(`${this.base}/api/apoderados`);
  }

  getById(id: number): Observable<Apoderado> {
    return this.http.get<Apoderado>(`${this.base}/api/apoderados/${id}`);
  }

  create(req: ApoderadoRequest): Observable<Apoderado> {
    return this.http.post<Apoderado>(`${this.base}/api/apoderados`, req);
  }

  update(id: number, req: ApoderadoRequest): Observable<Apoderado> {
    return this.http.put<Apoderado>(`${this.base}/api/apoderados/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/apoderados/${id}`);
  }
}
