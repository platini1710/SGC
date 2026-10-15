import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

/**
 * Cierra la sesión automáticamente tras un periodo de inactividad del usuario.
 * Se auto-gestiona observando currentUser$ del AuthService (sin dependencia circular:
 * AuthService NO conoce a este servicio). Al haber sesión arranca la vigilancia;
 * al no haberla, la detiene. Tiempo configurable vía parámetro timeoutInactividad.
 */
@Injectable({ providedIn: 'root' })
export class InactivityService {
  private authService = inject(AuthService);
  private zone = inject(NgZone);
  private http = inject(HttpClient);

  private timeoutMs = 60_000; // por defecto 60s; se actualiza desde el backend
  private timerId: ReturnType<typeof setTimeout> | null = null;
  private readonly eventos = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
  private readonly resetHandler = () => this.reiniciarTemporizador();
  private activo = false;

  // Debe llamarse una vez al arrancar la app (desde AppComponent).
  init(): void {
    this.authService.currentUser$.subscribe((usuario) => {
      if (usuario) {
        this.start();
      } else {
        this.stop();
      }
    });
  }

  private start(): void {
    if (this.activo) { return; }
    this.activo = true;

    // Lee el timeout configurable del backend; si falla usa el valor por defecto.
    this.http.get<{ timeoutInactividad?: number }>(`${environment.apiUrl}/api/parametros`).subscribe({
      next: (p) => {
        if (p?.timeoutInactividad && p.timeoutInactividad > 0) {
          this.timeoutMs = p.timeoutInactividad * 1000;
          if (this.activo) { this.reiniciarTemporizador(); }
        }
      },
      error: () => { /* mantiene el valor por defecto */ }
    });

    this.zone.runOutsideAngular(() => {
      this.eventos.forEach((e) => window.addEventListener(e, this.resetHandler, true));
      this.reiniciarTemporizador();
    });
  }

  private stop(): void {
    this.activo = false;
    this.eventos.forEach((e) => window.removeEventListener(e, this.resetHandler, true));
    this.limpiarTimer();
  }

  private reiniciarTemporizador(): void {
    this.limpiarTimer();
    this.timerId = setTimeout(() => this.expirar(), this.timeoutMs);
  }

  private limpiarTimer(): void {
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }
  }

  private expirar(): void {
    this.stop();
    this.zone.run(() => this.authService.logout('inactividad'));
  }
}
