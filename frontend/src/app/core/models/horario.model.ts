export interface HorarioCelda {
  horarioId: number;
  asignaturaId: number;
  nombreAsignatura: string;
  profesorId: number;
  nombreProfesor: string;
  color: string;
  nombreCurso?: string;
}

export interface GrillaHorario {
  cursoId?: number;
  profesorId?: number;
  nombreCurso?: string;
  nombreProfesor?: string;
  grilla: Record<string, Record<string, HorarioCelda | null>>;
}

export interface HorarioResponse {
  id: number;
  cursoId: number;
  nombreCurso: string;
  asignaturaId: number;
  nombreAsignatura: string;
  profesorId: number;
  nombreProfesor: string;
  diaSemana: number;
  bloqueId: number;
  horaInicio: string;
  horaFin: string;
  editadoManualmente: boolean;
}

export interface MoverBloqueRequest {
  diaSemana: number;
  bloqueId: number;
  profesorId: number;
}

export interface BloqueHorario {
  id: number;
  numero: number;
  horaInicio: string;
  horaFin: string;
  esRecreo: boolean;
}
