import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Descarga el PDF de notas. Si trimestreId es null -> reporte anual (los 3 trimestres).
  descargarPdf(cursoId: number, asignaturaId: number, trimestreId: number | null): Observable<Blob> {
    let params = new HttpParams()
      .set('cursoId', cursoId.toString())
      .set('asignaturaId', asignaturaId.toString());
    if (trimestreId != null) {
      params = params.set('trimestreId', trimestreId.toString());
    }
    return this.http.get(`${this.base}/api/reportes/notas/pdf`, {
      params,
      responseType: 'blob'
    });
  }

  // Descarga el "Informe de Avance de Notas Parciales" (boletín por alumno).
  // Sin estudianteId -> una página por cada alumno del curso.
  descargarInformeAlumno(cursoId: number, trimestreId: number, estudianteId?: number | null): Observable<Blob> {
    let params = new HttpParams()
      .set('cursoId', cursoId.toString())
      .set('trimestreId', trimestreId.toString());
    if (estudianteId != null) {
      params = params.set('estudianteId', estudianteId.toString());
    }
    return this.http.get(`${this.base}/api/reportes/alumno/pdf`, {
      params,
      responseType: 'blob'
    });
  }
}
