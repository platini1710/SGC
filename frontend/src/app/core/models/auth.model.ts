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
  // RF-01.2: true si la contraseña expiró o es alta inicial; el frontend fuerza el cambio.
  mustChangePassword?: boolean;
}

export interface UsuarioActual {
  username: string;
  rol: Rol;
  profesorId?: number;
  mustChangePassword?: boolean;
}
