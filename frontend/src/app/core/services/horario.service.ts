import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GrillaHorario, HorarioCelda, HorarioResponse, MoverBloqueRequest } from '../models/horario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HorarioService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getGrillaCurso(cursoId: number): Observable<GrillaHorario> {
    return this.http.get<GrillaHorario>(`${this.base}/api/cursos/${cursoId}/horario`);
  }

  // The backend serves GET /api/profesores/{id}/horario as a flat List<HorarioResponse>,
  // not a GrillaHorarioResponse. Fold that list into the nested grid shape the professor
  // grid expects, keyed by diaSemana (1..5) and block number (1..8). Blocks are seeded in
  // order so the bloqueId matches the block number.
  getGrillaProfesor(profesorId: number): Observable<GrillaHorario> {
    return this.http
      .get<HorarioResponse[]>(`${this.base}/api/profesores/${profesorId}/horario`)
      .pipe(map((horarios) => this.foldHorariosEnGrilla(profesorId, horarios)));
  }

  private foldHorariosEnGrilla(profesorId: number, horarios: HorarioResponse[]): GrillaHorario {
    const grilla: Record<string, Record<string, HorarioCelda | null>> = {};
    let nombreProfesor = '';

    for (const h of horarios) {
      nombreProfesor = h.nombreProfesor;
      const diaKey = String(h.diaSemana);
      const bloqueKey = String(h.bloqueId);
      if (!grilla[diaKey]) { grilla[diaKey] = {}; }
      const color = `#${(0xFFFFFF & Math.floor(h.asignaturaId * 2654435761)).toString(16).padStart(6, '0').toUpperCase()}`;
      grilla[diaKey][bloqueKey] = {
        horarioId: h.id,
        asignaturaId: h.asignaturaId,
        nombreAsignatura: h.nombreAsignatura,
        profesorId: h.profesorId,
        nombreProfesor: h.nombreProfesor,
        color,
        nombreCurso: h.nombreCurso
      };
    }

    return { profesorId, nombreProfesor, grilla };
  }

  generarHorario(): Observable<unknown> {
    return this.http.post(`${this.base}/api/horarios/generar`, {});
  }

  moverBloque(horarioId: number, req: MoverBloqueRequest): Observable<unknown> {
    return this.http.put(`${this.base}/api/horarios/${horarioId}`, req);
  }

  borrarHorario(): Observable<unknown> {
    return this.http.delete(`${this.base}/api/horarios`);
  }
}
