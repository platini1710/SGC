import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

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
  imports: [CommonModule, ReactiveFormsModule],
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

  ngOnInit(): void {
    // Aviso si la sesión se cerró por inactividad.
    this.avisoInactividad = this.route.snapshot.queryParamMap.get('motivo') === 'inactividad';

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

  get username() { return this.form.get('username')!; }
  get password() { return this.form.get('password')!; }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.login(this.form.value).subscribe({
      next: (res) => {
        if (res.rol === 'ADMIN') {
          this.router.navigate(['/horario/curso']);
        } else {
          this.router.navigate(['/notas']);
        }
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Credenciales inválidas';
      }
    });
  }
}
