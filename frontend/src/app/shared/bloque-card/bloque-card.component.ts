import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-bloque-card',
  standalone: true,
  imports: [],
  templateUrl: './bloque-card.component.html',
  styleUrl: './bloque-card.component.scss'
})
export class BloqueCardComponent {
  @Input() nombreAsignatura = '';
  @Input() nombreSecundario = '';
  @Input() color = '#1976D2';
  @Input() editable = false;
  @Output() editar = new EventEmitter<void>();
}
