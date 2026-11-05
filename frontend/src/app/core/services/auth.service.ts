import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Rol, UsuarioActual } from '../models/auth.model';

const TOKEN_KEY = 'sw_token';
const USER_KEY = 'sw_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private base = environment.apiUrl;
  readonly currentUser$ = new BehaviorSubject<UsuarioActual | null>(null);

  constructor() {
    const token = localStorage.getItem(TOKEN_KEY);
    const userRaw = localStorage.getItem(USER_KEY);
    if (token && userRaw) {
      try {
        this.currentUser$.next(JSON.parse(userRaw) as UsuarioActual);
      } catch {
        this.limpiarSesion();
      }
    }
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, req).pipe(
      tap((res) => this.guardarSesion(res))
    );
  }

  // motivo opcional: 'inactividad' cuando el cierre es automático.
  // Login por rostro: usuario + descriptor facial (128 valores). Misma respuesta que login().
  faceLogin(username: string, descriptor: number[]): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/face-login`, { username, descriptor }).pipe(
      tap((res) => this.guardarSesion(res))
    );
  }

  enrollFace(descriptor: number[], password: string, consentimiento: boolean): Observable<{ registrado: boolean }> {
    return this.http.post<{ registrado: boolean }>(
      `${this.base}/auth/face/enroll`, { descriptor, password, consentimiento });
  }

  faceStatus(): Observable<{ registrado: boolean }> {
    return this.http.get<{ registrado: boolean }>(`${this.base}/auth/face/status`);
  }

  removeFace(): Observable<void> {
    return this.http.delete<void>(`${this.base}/auth/face`);
  }

  // RF-01.3: solicita el código de 5 dígitos al correo del profesor.
  solicitarCodigoCambio(email: string): Observable<{ message: string; debugCode?: string }> {
    return this.http.post<{ message: string; debugCode?: string }>(
      `${this.base}/auth/pwd/code`, { email });
  }

  // RF-01.3: valida el código y asienta la nueva contraseña.
  confirmarCambioPassword(email: string, code: string, newPassword: string): Observable<{ updated: boolean }> {
    return this.http.post<{ updated: boolean }>(
      `${this.base}/auth/pwd/reset`, { email, code, newPassword });
  }

  // El login quedó a medias: hay sesión, pero el docente debe cambiar la clave antes de usarla.
  requiereCambioPassword(): boolean {
    return !!this.getUsuario()?.mustChangePassword;
  }

  private guardarSesion(res: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    const usuario: UsuarioActual = {
      username: res.username,
      rol: res.rol,
      profesorId: res.profesorId,
      mustChangePassword: res.mustChangePassword
    };
    localStorage.setItem(USER_KEY, JSON.stringify(usuario));
    this.currentUser$.next(usuario);
  }

  logout(motivo?: 'inactividad'): void {
    this.limpiarSesion();
    const extras = motivo === 'inactividad'
      ? { queryParams: { motivo: 'inactividad' } }
      : {};
    this.router.navigate(['/login'], extras);
  }

  private limpiarSesion(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser$.next(null);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUsuario(): UsuarioActual | null {
    return this.currentUser$.getValue();
  }

  getRol(): Rol | null {
    return this.currentUser$.getValue()?.rol ?? null;
  }
}
