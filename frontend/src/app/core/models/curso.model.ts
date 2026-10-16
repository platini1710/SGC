export interface Curso {
  id: number;
  nombre: string;
  nivel: number;
  profesorJefe?: string;
}

export interface CursoDetalle extends Curso {
  profesorJefeId?: number | null;
  nombreProfesorJefe?: string | null;
  asignaturas: CursoAsignatura[];
}

export interface CursoAsignatura {
  asignaturaId: number;
  nombreAsignatura: string;
  horasSemanales: number;
  profesorId: number | null;
  nombreProfesor: string | null;
}

export interface CursoAsignaturaRequest {
  asignaturaId: number;
  horasSemanales: number;
  profesorId?: number | null;
}
