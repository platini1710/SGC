// Modelos de matrícula por año, espejo de los DTOs del backend.
export type EstadoMatricula = 'ACTIVA' | 'RETIRADO';

export interface MatriculaRequest {
  estudianteId: number;
  cursoId: number;
  anio: number;
  estado: string;
}

export interface MatriculaResponse {
  id: number;
  estudianteId: number;
  nombreEstudiante: string;
  cursoId: number;
  nombreCurso: string;
  anio: number;
  estado: string;
}
