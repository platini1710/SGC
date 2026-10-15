import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss'
})
export class NavComponent {
  authService = inject(AuthService);

  // Estado del submenú colapsable "Mantenedores".
  mantenedoresAbierto = false;

  get esAdmin(): boolean {
    return this.authService.getRol() === 'ADMIN';
  }

  get username(): string {
    return this.authService.getUsuario()?.username ?? '';
  }

  toggleMantenedores(): void {
    this.mantenedoresAbierto = !this.mantenedoresAbierto;
  }

  logout(): void {
    this.authService.logout();
  }
}
