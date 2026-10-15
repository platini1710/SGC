export interface Profesor {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rut: string;
  activo?: boolean;
  especialidades: string[];
}

export interface ProfesorRequest {
  nombre: string;
  apellido: string;
  email?: string;
  rut?: string;
  especialidadIds: number[];
}
