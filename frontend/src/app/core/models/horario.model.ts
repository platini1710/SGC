export interface HorarioCelda {
  horarioId: number;
  asignaturaId: number;
  nombreAsignatura: string;
  profesorId: number;
  nombreProfesor: string;
  color: string;
  nombreCurso?: string;
  // Segundo subtramo opcional (medios bloques JEC).
  asignaturaSecId?: number;
  nombreAsignaturaSec?: string;
  profesorSecId?: number;
  nombreProfesorSec?: string;
  esMedioBloque?: boolean;
}

export interface BloqueInfo {
  id: number;
  numero: number;
  jornada: string;
  horaInicio: string;
  horaFin: string;
  esRecreo: boolean;
  claveRender: string;
}

export interface GrillaHorario {
  cursoId?: number;
  profesorId?: number;
  nombreCurso?: string;
  nombreProfesor?: string;
  grilla: Record<string, Record<string, HorarioCelda | null>>;
  bloques?: BloqueInfo[];
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
  bloqueNumero: number;
  jornada: string;
  horaInicio: string;
  horaFin: string;
  editadoManualmente: boolean;
  // Segundo subtramo opcional (medios bloques JEC).
  asignaturaSecId?: number;
  nombreAsignaturaSec?: string;
  profesorSecId?: number;
  nombreProfesorSec?: string;
  esMedioBloque?: boolean;
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
