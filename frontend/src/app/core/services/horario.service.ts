import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { BloqueInfo, GrillaHorario, HorarioCelda, HorarioResponse, MoverBloqueRequest } from '../models/horario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HorarioService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // El payload del curso ya trae su propia lista de bloques (filtrada por jornada en el backend).
  getGrillaCurso(cursoId: number): Observable<GrillaHorario> {
    return this.http.get<GrillaHorario>(`${this.base}/api/cursos/${cursoId}/horario`);
  }

  // Devuelve la unión de ambos juegos de bloques (JEC + REGULAR), ordenada (jornada, numero).
  getBloques(): Observable<BloqueInfo[]> {
    return this.http.get<BloqueInfo[]>(`${this.base}/api/horarios/bloques`);
  }

  // El backend sirve GET /api/profesores/{id}/horario como una lista plana de HorarioResponse
  // (no una grilla). Un profesor cruza jornadas, así que indexamos por la clave compuesta
  // "<jornada>-<bloqueNumero>" y componemos las filas con la unión de bloques del payload.
  getGrillaProfesor(profesorId: number): Observable<GrillaHorario> {
    return forkJoin({
      horarios: this.http.get<HorarioResponse[]>(`${this.base}/api/profesores/${profesorId}/horario`),
      bloques: this.getBloques()
    }).pipe(map(({ horarios, bloques }) => this.foldHorariosEnGrilla(profesorId, horarios, bloques)));
  }

  private foldHorariosEnGrilla(profesorId: number, horarios: HorarioResponse[], bloques: BloqueInfo[]): GrillaHorario {
    const grilla: Record<string, Record<string, HorarioCelda | null>> = {};
    let nombreProfesor = '';

    for (const h of horarios) {
      nombreProfesor = h.nombreProfesor;
      const diaKey = String(h.diaSemana);
      // Clave compuesta jornada-numero (= BloqueInfo.claveRender), única entre jornadas.
      const bloqueKey = `${h.jornada}-${h.bloqueNumero}`;
      if (!grilla[diaKey]) { grilla[diaKey] = {}; }
      const color = `#${(0xFFFFFF & Math.floor(h.asignaturaId * 2654435761)).toString(16).padStart(6, '0').toUpperCase()}`;
      grilla[diaKey][bloqueKey] = {
        horarioId: h.id,
        asignaturaId: h.asignaturaId,
        nombreAsignatura: h.nombreAsignatura,
        profesorId: h.profesorId,
        nombreProfesor: h.nombreProfesor,
        color,
        nombreCurso: h.nombreCurso,
        asignaturaSecId: h.asignaturaSecId,
        nombreAsignaturaSec: h.nombreAsignaturaSec,
        profesorSecId: h.profesorSecId,
        nombreProfesorSec: h.nombreProfesorSec,
        esMedioBloque: h.esMedioBloque
      };
    }

    return { profesorId, nombreProfesor, grilla, bloques };
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
