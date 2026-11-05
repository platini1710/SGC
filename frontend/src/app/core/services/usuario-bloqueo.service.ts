import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UsuarioBloqueado {
  id: number;
  username: string;
  nombre: string | null;
  rol: string;
  // true: bloqueo permanente; false: temporal hasta bloqueadoHasta.
  permanente: boolean;
  bloqueadoHasta: string | null;
}

@Injectable({ providedIn: 'root' })
export class UsuarioBloqueoService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getBloqueados(): Observable<UsuarioBloqueado[]> {
    return this.http.get<UsuarioBloqueado[]>(`${this.base}/api/usuarios/bloqueados`);
  }

  desbloquear(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/api/usuarios/${id}/desbloquear`, {});
  }
}
