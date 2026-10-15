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
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.token);
        const usuario: UsuarioActual = {
          username: res.username,
          rol: res.rol,
          profesorId: res.profesorId
        };
        localStorage.setItem(USER_KEY, JSON.stringify(usuario));
        this.currentUser$.next(usuario);
      })
    );
  }

  // motivo opcional: 'inactividad' cuando el cierre es automático.
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
