import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Curso, CursoDetalle, CursoAsignaturaRequest } from '../models/curso.model';
import { Asignatura } from '../models/asignatura.model';
import { GrillaHorario } from '../models/horario.model';
import { Estudiante } from '../models/estudiante.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CursoService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Curso[]> {
    return this.http.get<Curso[]>(`${this.base}/api/cursos`);
  }

  getById(id: number): Observable<CursoDetalle> {
    return this.http.get<CursoDetalle>(`${this.base}/api/cursos/${id}`);
  }

  getHorario(id: number): Observable<GrillaHorario> {
    return this.http.get<GrillaHorario>(`${this.base}/api/cursos/${id}/horario`);
  }

  addAsignatura(cursoId: number, req: CursoAsignaturaRequest): Observable<CursoDetalle> {
    return this.http.post<CursoDetalle>(`${this.base}/api/cursos/${cursoId}/asignaturas`, req);
  }

  removeAsignatura(cursoId: number, asignaturaId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/cursos/${cursoId}/asignaturas/${asignaturaId}`);
  }

  getEstudiantes(cursoId: number, page = 0, size = 200): Observable<PageResponse<Estudiante>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Estudiante>>(
      `${this.base}/api/cursos/${cursoId}/estudiantes`, { params });
  }

  // Asigna (profesorId) o quita (null) el profesor jefe del curso.
  asignarProfesorJefe(cursoId: number, profesorId: number | null): Observable<Curso> {
    return this.http.put<Curso>(`${this.base}/api/cursos/${cursoId}/profesor-jefe`, { profesorId });
  }

  // Asigna (planEstudioId) o quita (null) el plan de estudio del curso.
  asignarPlanEstudio(cursoId: number, planEstudioId: number | null): Observable<Curso> {
    return this.http.put<Curso>(`${this.base}/api/cursos/${cursoId}/plan-estudio`, { planEstudioId });
  }

  // Asignaturas de la malla del plan asociado al curso (vacío si no tiene plan).
  getAsignaturasDisponibles(cursoId: number): Observable<Asignatura[]> {
    return this.http.get<Asignatura[]>(`${this.base}/api/cursos/${cursoId}/asignaturas-disponibles`);
  }
}
