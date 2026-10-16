export interface Estudiante {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  // Derivado (apellidoPaterno [+ ' ' + apellidoMaterno]) para compatibilidad.
  apellido?: string;
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
  // Estado y auditoría
  activo?: boolean;
  usuarioModificacion?: string;
  fechaModificacion?: string;
  horaModificacion?: string;
  inactivadoPor?: string;
  fechaInactivacion?: string;
  horaInactivacion?: string;
}

export interface EstudianteRequest {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
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
