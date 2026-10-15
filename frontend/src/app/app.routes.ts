import { Routes } from '@angular/router';
import { GrillaCursoComponent } from './features/horario/grilla-curso/grilla-curso.component';
import { GrillaProfesorComponent } from './features/horario/grilla-profesor/grilla-profesor.component';
import { HorarioAdminComponent } from './features/horario/horario-admin/horario-admin.component';
import { ProfesoresListComponent } from './features/profesores/profesores-list.component';
import { AsignaturasListComponent } from './features/asignaturas/asignaturas-list.component';
import { CursosListComponent } from './features/cursos/cursos-list.component';
import { EstudiantesListComponent } from './features/estudiantes/estudiantes-list.component';
import { ApoderadosListComponent } from './features/apoderados/apoderados-list.component';
import { GrillaNotasComponent } from './features/notas/grilla-notas/grilla-notas.component';
import { ConsolidadoComponent } from './features/notas/consolidado/consolidado.component';
import { TrimestresComponent } from './features/notas/trimestres/trimestres.component';
import { ReportesComponent } from './features/reportes/reportes.component';
import { ParametrosComponent } from './features/parametros/parametros.component';
import { LoginComponent } from './features/auth/login/login.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'horario/curso', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'horario/curso', component: GrillaCursoComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'horario/profesor', component: GrillaProfesorComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'horario/admin', component: HorarioAdminComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'profesores', component: ProfesoresListComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'asignaturas', component: AsignaturasListComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'cursos', component: CursosListComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'estudiantes', component: EstudiantesListComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'apoderados', component: ApoderadosListComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'trimestres', component: TrimestresComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'notas', component: GrillaNotasComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'DOCENTE'] } },
  { path: 'consolidado', component: ConsolidadoComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'DOCENTE'] } },
  { path: 'reportes', component: ReportesComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'DOCENTE'] } },
  { path: 'parametros', component: ParametrosComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: '**', redirectTo: 'horario/curso' }
];
