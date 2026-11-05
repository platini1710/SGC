import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

// Misma regla del backend (RF-01.1): mínimo 6, al menos un número y un carácter especial.
const PWD_REGEX = /^(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{6,}$/;
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

@Component({
  selector: 'app-cambiar-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './cambiar-password.component.html',
  styleUrl: './cambiar-password.component.scss'
})
export class CambiarPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Paso 1: pedir el código. Paso 2: ingresar código + nueva contraseña.
  paso: 1 | 2 = 1;
  // Aviso cuando se llega aquí por contraseña caducada.
  expirada = false;
  cargando = false;
  mensajeOk = '';
  errorMsg = '';
  // Código mostrado en modo demo (sin SMTP). Null en producción.
  debugCode: string | null = null;

  emailForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.pattern(EMAIL_REGEX)]]
  });

  resetForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
    newPassword: ['', [Validators.required, Validators.pattern(PWD_REGEX)]]
  });

  ngOnInit(): void {
    this.expirada = this.route.snapshot.queryParamMap.get('motivo') === 'expirada';
  }

  get email() { return this.emailForm.get('email')!; }
  get code() { return this.resetForm.get('code')!; }
  get newPassword() { return this.resetForm.get('newPassword')!; }

  solicitarCodigo(): void {
    if (this.emailForm.invalid) { this.emailForm.markAllAsTouched(); return; }
    this.cargando = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    this.authService.solicitarCodigoCambio(this.email.value).subscribe({
      next: (r) => {
        this.cargando = false;
        this.paso = 2;
        this.mensajeOk = r.message;
        this.debugCode = r.debugCode ?? null;
      },
      error: () => {
        this.cargando = false;
        this.errorMsg = 'No se pudo solicitar el código. Intente nuevamente.';
      }
    });
  }

  reenviar(): void {
    this.solicitarCodigo();
  }

  confirmar(): void {
    if (this.resetForm.invalid) { this.resetForm.markAllAsTouched(); return; }
    this.cargando = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    this.authService.confirmarCambioPassword(this.email.value, this.code.value, this.newPassword.value).subscribe({
      next: () => {
        this.cargando = false;
        // La sesión previa pudo quedar a medias: se limpia y se vuelve al login.
        this.authService.logout();
        this.router.navigate(['/login'], { queryParams: { cambio: 'ok' } });
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.errorMsg = err?.error?.error || 'No se pudo cambiar la contraseña.';
      }
    });
  }
}
