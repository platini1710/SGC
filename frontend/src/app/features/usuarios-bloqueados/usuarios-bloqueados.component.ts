import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { UsuarioBloqueado, UsuarioBloqueoService } from '../../core/services/usuario-bloqueo.service';

@Component({
  selector: 'app-usuarios-bloqueados',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './usuarios-bloqueados.component.html'
})
export class UsuariosBloqueadosComponent implements OnInit {
  private service = inject(UsuarioBloqueoService);

  usuarios: UsuarioBloqueado[] = [];
  cargando = false;
  desbloqueandoId: number | null = null;
  mensajeOk = '';
  errorMsg = '';

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorMsg = '';
    this.service.getBloqueados().subscribe({
      next: (u) => { this.usuarios = u; this.cargando = false; },
      error: () => { this.errorMsg = 'Error cargando los usuarios bloqueados'; this.cargando = false; }
    });
  }

  desbloquear(u: UsuarioBloqueado): void {
    this.desbloqueandoId = u.id;
    this.mensajeOk = '';
    this.errorMsg = '';
    this.service.desbloquear(u.id).subscribe({
      next: () => {
        this.desbloqueandoId = null;
        this.usuarios = this.usuarios.filter((x) => x.id !== u.id);
        this.mensajeOk = `Usuario ${u.nombre ?? u.username} (${u.username}) desbloqueado.`;
      },
      error: () => {
        this.desbloqueandoId = null;
        this.errorMsg = `No se pudo desbloquear al usuario ${u.username}.`;
      }
    });
  }
}
