import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, inject } from '@angular/core';
import { FaceService } from '../../core/services/face.service';

/**
 * Muestra la cámara y, al pulsar el botón, emite el descriptor facial (128 valores).
 * La cámara se activa al crearse el componente y se libera al destruirse (usar @if).
 */
@Component({
  selector: 'app-face-capture',
  standalone: true,
  templateUrl: './face-capture.component.html',
  styleUrl: './face-capture.component.scss'
})
export class FaceCaptureComponent implements AfterViewInit, OnDestroy {
  private face = inject(FaceService);

  @Input() buttonLabel = 'Capturar rostro';
  @Input() busy = false;
  @Output() captured = new EventEmitter<number[]>();

  @ViewChild('video', { static: true }) videoRef!: ElementRef<HTMLVideoElement>;

  ready = false;
  capturing = false;
  status = 'Iniciando cámara...';
  error = '';

  async ngAfterViewInit(): Promise<void> {
    try {
      await this.face.prepare();
      await this.face.startCamera(this.videoRef.nativeElement);
      this.ready = true;
      this.status = 'Mira a la cámara y pulsa el botón.';
    } catch (e) {
      this.error = FaceService.messageFor(e);
      this.status = '';
    }
  }

  async capture(): Promise<void> {
    if (!this.ready || this.capturing || this.busy) { return; }
    this.capturing = true;
    this.error = '';
    this.status = 'Analizando rostro...';
    try {
      const descriptor = await this.face.captureDescriptor(this.videoRef.nativeElement);
      this.status = '';
      this.captured.emit(descriptor);
    } catch (e) {
      this.error = FaceService.messageFor(e);
      this.status = 'Mira a la cámara y pulsa el botón.';
    } finally {
      this.capturing = false;
    }
  }

  ngOnDestroy(): void {
    this.face.stopCamera(this.videoRef?.nativeElement);
  }
}
