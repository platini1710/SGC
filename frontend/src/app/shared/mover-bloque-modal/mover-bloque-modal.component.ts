import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HorarioService } from '../../core/services/horario.service';
import { ProfesorService } from '../../core/services/profesor.service';
import { Profesor } from '../../core/models/profesor.model';

const DIAS = [
  { value: 1, label: 'Lunes' },
  { value: 2, label: 'Martes' },
  { value: 3, label: 'Miércoles' },
  { value: 4, label: 'Jueves' },
  { value: 5, label: 'Viernes' },
];

const BLOQUES_SELECCIONABLES = [
  { value: 1, label: 'Bloque 1 (08:00–08:45)' },
  { value: 2, label: 'Bloque 2 (08:45–09:30)' },
  { value: 3, label: 'Bloque 3 (09:30–10:15)' },
  { value: 5, label: 'Bloque 5 (10:45–11:30)' },
  { value: 6, label: 'Bloque 6 (11:30–12:15)' },
  { value: 7, label: 'Bloque 7 (12:15–13:00)' },
  { value: 8, label: 'Bloque 8 (13:00–13:45)' },
];

@Component({
  selector: 'app-mover-bloque-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './mover-bloque-modal.component.html',
  styleUrl: './mover-bloque-modal.component.scss'
})
export class MoverBloqueModalComponent implements OnInit {
  @Input() horarioId = 0;
  @Input() diaSemanaActual = 1;
  @Input() bloqueActual = 1;
  @Input() profesorActual = 0;
  @Output() guardado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private horarioService = inject(HorarioService);
  private profesorService = inject(ProfesorService);

  form!: FormGroup;
  profesores: Profesor[] = [];
  dias = DIAS;
  bloques = BLOQUES_SELECCIONABLES;
  guardando = false;
  errorMsg = '';

  ngOnInit(): void {
    this.form = this.fb.group({
      diaSemana: [this.diaSemanaActual, Validators.required],
      bloqueId: [this.bloqueActual, Validators.required],
      profesorId: [this.profesorActual, Validators.required]
    });
    this.profesorService.getAll().subscribe(p => (this.profesores = p));
  }

  guardar(): void {
    if (this.form.invalid) { return; }
    this.guardando = true;
    this.errorMsg = '';
    const req = {
      diaSemana: Number(this.form.value['diaSemana']),
      bloqueId: Number(this.form.value['bloqueId']),
      profesorId: Number(this.form.value['profesorId'])
    };
    this.horarioService.moverBloque(this.horarioId, req).subscribe({
      next: () => {
        this.guardando = false;
        this.guardado.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando = false;
        if (err.status === 409) {
          this.errorMsg = 'Conflicto: ese bloque ya está ocupado para el profesor o el curso en ese horario.';
        } else {
          this.errorMsg = `Error al mover bloque: ${err.message}`;
        }
      }
    });
  }
}
