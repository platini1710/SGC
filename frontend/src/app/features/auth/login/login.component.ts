import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';
import { FaceCaptureComponent } from '../../../shared/face-capture/face-capture.component';

interface Colegio {
  nombre: string;
  region: string;
  comuna: string;
  direccion: string;
  logo: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FaceCaptureComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  colegio: Colegio | null = null;
  avisoInactividad = false;
  avisoCambioOk = false;

  ngOnInit(): void {
    // Aviso si la sesión se cerró por inactividad.
    this.avisoInactividad = this.route.snapshot.queryParamMap.get('motivo') === 'inactividad';
    this.avisoCambioOk = this.route.snapshot.queryParamMap.get('cambio') === 'ok';

    // Asegura campos vacíos al mostrar el login (no recuerda el usuario anterior).
    this.form.reset({ username: '', password: '' });

    // Carga pública de los datos del colegio para mostrarlos en el login.
    this.http.get<Colegio>(`${environment.apiUrl}/api/colegio`).subscribe({
      next: (c) => (this.colegio = c),
      error: () => (this.colegio = null)
    });
  }

  form: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  isLoading = false;
  errorMessage = '';
  showFace = false;
  // El enlace de recuperación solo aparece tras el 3.er fallo de contraseña (429).
  mostrarRecuperar = false;

  get username() { return this.form.get('username')!; }
  get password() { return this.form.get('password')!; }

  private irAInicio(rol: string): void {
    // RF-01.2: contraseña expirada o alta inicial → forzar el cambio antes de entrar.
    if (this.authService.requiereCambioPassword()) {
      this.router.navigate(['/cambiar-password'], { queryParams: { motivo: 'expirada' } });
      return;
    }
    this.router.navigate([rol === 'ADMIN' ? '/horario/curso' : '/notas']);
  }

  // Abre/cierra el panel de cámara. El usuario es necesario porque la verificación es 1:1.
  toggleFace(): void {
    if (this.showFace) {
      this.showFace = false;
      return;
    }
    this.errorMessage = '';
    if (this.username.invalid) {
      this.username.markAsTouched();
      this.errorMessage = 'Ingresa tu usuario antes de usar el reconocimiento facial.';
      return;
    }
    this.showFace = true;
  }

  onFaceCaptured(descriptor: number[]): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.faceLogin(this.username.value, descriptor).subscribe({
      next: (res) => this.irAInicio(res.rol),
      error: (err) => {
        this.isLoading = false;
        if (err?.status === 429) {
          // Cuenta bloqueada: cierra la cámara, ya no tiene sentido seguir intentando.
          this.showFace = false;
          this.errorMessage = err?.error?.error || 'Cuenta bloqueada por intentos fallidos. Contacte al administrador.';
        } else {
          this.errorMessage = 'No se reconoció el rostro. Inténtalo de nuevo o usa tu contraseña.';
        }
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.mostrarRecuperar = false;
    this.authService.login(this.form.value).subscribe({
      next: (res) => this.irAInicio(res.rol),
      error: (err) => {
        this.isLoading = false;
        // 429 = tercer intento fallido de contraseña sobre una cuenta existente.
        // Solo entonces ofrecemos el enlace de recuperación. Un usuario inexistente
        // nunca llega a 429 (su contador no incrementa), así que no verá el enlace.
        if (err?.status === 429) {
          this.mostrarRecuperar = true;
          this.errorMessage = err?.error?.error
            || 'Cuenta bloqueada por 3 intentos fallidos. Puede restablecer su contraseña.';
        } else {
          this.errorMessage = 'Credenciales inválidas';
        }
      }
    });
  }
}
