export interface Especialidad {
  id: number;
  nombre: string;
  descripcion: string;
}

export interface EspecialidadRequest {
  nombre: string;
  descripcion?: string;
}
