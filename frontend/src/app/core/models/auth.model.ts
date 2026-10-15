export type Rol = 'ADMIN' | 'DOCENTE';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  rol: Rol;
  profesorId?: number;
  tokenType?: string;
}

export interface UsuarioActual {
  username: string;
  rol: Rol;
  profesorId?: number;
}
