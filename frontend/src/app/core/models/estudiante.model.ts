export interface Estudiante {
  id: number;
  nombre: string;
  apellido: string;
  rut: string;
  fechaNacimiento?: string;
  cursoId?: number;
  nombreCurso?: string;
  apoderadoId?: number;
  nombreApoderado?: string;
}

export interface EstudianteRequest {
  nombre: string;
  apellido: string;
  rut: string;
  fechaNacimiento?: string;
  cursoId?: number | null;
  apoderadoId?: number | null;
}
