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
  // Identificación
  lugarNacimiento?: string;
  nacionalidad?: string;
  sexo?: string;
  comuna?: string;
  telefono?: string;
  domicilio?: string;
  foto?: string;
  // Antecedentes familiares
  padreNombre?: string;
  padreRut?: string;
  padreOcupacion?: string;
  madreNombre?: string;
  madreRut?: string;
  madreOcupacion?: string;
  apoderadoRelacion?: string;
  // Información médica
  prevision?: string;
  alergias?: string;
  condicionMedica?: string;
  contactoEmergencia?: string;
  fonoEmergencia?: string;
}

export interface EstudianteRequest {
  nombre: string;
  apellido: string;
  rut: string;
  fechaNacimiento?: string;
  cursoId?: number | null;
  apoderadoId?: number | null;
  // Identificación
  lugarNacimiento?: string;
  nacionalidad?: string;
  sexo?: string;
  comuna?: string;
  telefono?: string;
  domicilio?: string;
  foto?: string;
  // Antecedentes familiares
  padreNombre?: string;
  padreRut?: string;
  padreOcupacion?: string;
  madreNombre?: string;
  madreRut?: string;
  madreOcupacion?: string;
  apoderadoRelacion?: string;
  // Información médica
  prevision?: string;
  alergias?: string;
  condicionMedica?: string;
  contactoEmergencia?: string;
  fonoEmergencia?: string;
}
