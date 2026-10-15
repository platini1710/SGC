import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Trimestre } from '../models/trimestre.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TrimestreService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(anio?: number): Observable<Trimestre[]> {
    let params = new HttpParams();
    if (anio !== undefined) {
      params = params.set('anio', anio.toString());
    }
    return this.http.get<Trimestre[]>(`${this.base}/api/trimestres`, { params });
  }

  abrir(id: number): Observable<Trimestre> {
    return this.http.put<Trimestre>(`${this.base}/api/trimestres/${id}/abrir`, {});
  }

  cerrar(id: number): Observable<Trimestre> {
    return this.http.put<Trimestre>(`${this.base}/api/trimestres/${id}/cerrar`, {});
  }
}
