import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { FaceCaptureComponent } from '../../../shared/face-capture/face-capture.component';

@Component({
  selector: 'app-mi-rostro',
  standalone: true,
  imports: [FormsModule, FaceCaptureComponent],
  templateUrl: './mi-rostro.component.html',
  styleUrl: './mi-rostro.component.scss'
})
export class MiRostroComponent implements OnInit {
  private authService = inject(AuthService);

  registrado: boolean | null = null;
  mostrarFormulario = false;
  password = '';
  consentimiento = false;
  ocupado = false;
  mensajeOk = '';
  errorMsg = '';

  ngOnInit(): void {
    this.authService.faceStatus().subscribe({
      next: (s) => (this.registrado = s.registrado),
      error: () => (this.errorMsg = 'No se pudo consultar el estado del rostro.')
    });
  }

  toggleFormulario(): void {
    this.mostrarFormulario = !this.mostrarFormulario;
    this.mensajeOk = '';
    this.errorMsg = '';
    this.password = '';
    this.consentimiento = false;
  }

  // La cámara solo se activa con contraseña y consentimiento completos.
  get puedeCapturar(): boolean {
    return this.password.length > 0 && this.consentimiento;
  }

  onCapturado(descriptor: number[]): void {
    if (!this.puedeCapturar) {
      this.errorMsg = 'Ingresa tu contraseña y acepta el uso de datos biométricos.';
      return;
    }
    this.ocupado = true;
    this.errorMsg = '';
    this.authService.enrollFace(descriptor, this.password, this.consentimiento).subscribe({
      next: () => {
        this.ocupado = false;
        this.registrado = true;
        this.mostrarFormulario = false;
        this.password = '';
        this.consentimiento = false;
        this.mensajeOk = 'Rostro registrado correctamente.';
      },
      error: (err: HttpErrorResponse) => {
        this.ocupado = false;
        this.errorMsg = err.status === 403
          ? 'La contraseña es incorrecta.'
          : (err.error?.error ?? 'No se pudo registrar el rostro.');
      }
    });
  }

  eliminar(): void {
    this.ocupado = true;
    this.errorMsg = '';
    this.mensajeOk = '';
    this.authService.removeFace().subscribe({
      next: () => {
        this.ocupado = false;
        this.registrado = false;
        this.mensajeOk = 'Rostro eliminado.';
      },
      error: () => {
        this.ocupado = false;
        this.errorMsg = 'No se pudo eliminar el rostro.';
      }
    });
  }
}
