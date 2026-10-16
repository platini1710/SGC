import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PromocionPreviewRequest,
  PromocionPreviewResponse,
  PromocionConfirmRequest,
  PromocionResultadoResponse
} from '../models/promocion.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MatriculaService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Previsualiza la promoción sin escribir nada.
  promoverPreview(body: PromocionPreviewRequest): Observable<PromocionPreviewResponse> {
    return this.http.post<PromocionPreviewResponse>(
      `${this.base}/api/matriculas/promover/preview`, body);
  }

  // Confirma la promoción (upsert idempotente de matrículas del año destino).
  promoverConfirmar(body: PromocionConfirmRequest): Observable<PromocionResultadoResponse> {
    return this.http.post<PromocionResultadoResponse>(
      `${this.base}/api/matriculas/promover/confirmar`, body);
  }
}
