import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Rol } from '../models/auth.model';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const rolesPermitidos = (route.data?.['roles'] as Rol[]) ?? [];
  const rol = authService.getRol();

  if (rol && rolesPermitidos.includes(rol)) {
    return true;
  }

  // Reenvía a la ruta por defecto según el rol actual.
  if (rol === 'DOCENTE') {
    router.navigate(['/notas']);
  } else if (rol === 'ADMIN') {
    router.navigate(['/horario/curso']);
  } else {
    router.navigate(['/login']);
  }
  return false;
};
