export interface Asignatura {
  id: number;
  nombre: string;
  codigo: string;
  descripcion: string;
}

export interface AsignaturaRequest {
  nombre: string;
  codigo?: string;
  descripcion?: string;
}
