export interface NotaResponse {
  id: number;
  posicion: number;
  valor: number;
}

export interface FilaNota {
  estudianteId: number;
  nombreEstudiante: string;
  notas: NotaResponse[];
  promedio: number | null;
}

export interface GrillaNotas {
  cursoId: number;
  nombreCurso: string;
  asignaturaId: number;
  nombreAsignatura: string;
  trimestreId: number;
  numeroTrimestre: number;
  estadoTrimestre: string;
  filas: FilaNota[];
}

export interface FilaConsolidado {
  estudianteId: number;
  nombreEstudiante: string;
  promedioT1: number | null;
  promedioT2: number | null;
  promedioT3: number | null;
  notaFinal: number | null;
}

export interface ConsolidadoAnual {
  cursoId: number;
  asignaturaId: number;
  anio: number;
  filas: FilaConsolidado[];
}

export interface NotaRequest {
  estudianteId: number;
  asignaturaId: number;
  trimestreId: number;
  posicion: number;
  valor: number;
}

export interface AsignacionDocente {
  cursoId: number;
  nombreCurso: string;
  asignaturaId: number;
  nombreAsignatura: string;
}
