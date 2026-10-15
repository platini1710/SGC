export interface Apoderado {
  id: number;
  rut: string;
  nombre: string;
  apellidoPaterno: string;
  region?: string;
  comuna?: string;
  direccion?: string;
  telefonoCelular?: string;
  correo: string;
  cantidadEstudiantes: number;
}

export interface ApoderadoRequest {
  rut: string;
  nombre: string;
  apellidoPaterno: string;
  region?: string;
  comuna?: string;
  direccion?: string;
  telefonoCelular?: string;
  correo?: string;
}
