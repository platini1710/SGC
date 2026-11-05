import { Injectable } from '@angular/core';

type FaceApi = typeof import('@vladmandic/face-api');

export type FaceErrorCode = 'CAMERA_DENIED' | 'CAMERA_UNAVAILABLE' | 'MODELS_FAILED' | 'NO_FACE' | 'MULTIPLE_FACES';

export class FaceError extends Error {
  constructor(public readonly code: FaceErrorCode) {
    super(code);
  }
}

const MODELS_URL = '/assets/models';
const MAX_ATTEMPTS = 20;
const ATTEMPT_DELAY_MS = 200;

@Injectable({ providedIn: 'root' })
export class FaceService {
  private loading: Promise<FaceApi> | null = null;
  private stream: MediaStream | null = null;

  // Carga la librería (chunk diferido) y los modelos una sola vez.
  private loadApi(): Promise<FaceApi> {
    if (!this.loading) {
      this.loading = (async () => {
        const faceapi = await import('@vladmandic/face-api');
        await Promise.all([
          faceapi.nets.tinyFaceDetector.loadFromUri(MODELS_URL),
          faceapi.nets.faceLandmark68Net.loadFromUri(MODELS_URL),
          faceapi.nets.faceRecognitionNet.loadFromUri(MODELS_URL)
        ]);
        return faceapi;
      })().catch(() => {
        this.loading = null;
        throw new FaceError('MODELS_FAILED');
      });
    }
    return this.loading;
  }

  async prepare(): Promise<void> {
    await this.loadApi();
  }

  async startCamera(video: HTMLVideoElement): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new FaceError('CAMERA_UNAVAILABLE');
    }
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
        audio: false
      });
    } catch (e) {
      const name = (e as DOMException)?.name;
      throw new FaceError(name === 'NotAllowedError' || name === 'SecurityError'
        ? 'CAMERA_DENIED' : 'CAMERA_UNAVAILABLE');
    }
    video.srcObject = this.stream;
    await video.play();
  }

  stopCamera(video?: HTMLVideoElement): void {
    this.stream?.getTracks().forEach(t => t.stop());
    this.stream = null;
    if (video) {
      video.srcObject = null;
    }
  }

  // Toma varias muestras del rostro y devuelve el descriptor promedio (128 valores).
  async captureDescriptor(video: HTMLVideoElement, samples = 3): Promise<number[]> {
    const faceapi = await this.loadApi();
    const options = new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 });
    const collected: Float32Array[] = [];

    for (let i = 0; i < MAX_ATTEMPTS && collected.length < samples; i++) {
      const results = await faceapi
        .detectAllFaces(video, options)
        .withFaceLandmarks()
        .withFaceDescriptors();
      if (results.length > 1) {
        throw new FaceError('MULTIPLE_FACES');
      }
      if (results.length === 1) {
        collected.push(results[0].descriptor);
      }
      await new Promise(resolve => setTimeout(resolve, ATTEMPT_DELAY_MS));
    }

    if (collected.length < samples) {
      throw new FaceError('NO_FACE');
    }
    const size = collected[0].length;
    const avg = new Array<number>(size).fill(0);
    for (const d of collected) {
      for (let j = 0; j < size; j++) {
        avg[j] += d[j] / collected.length;
      }
    }
    return avg;
  }

  static messageFor(err: unknown): string {
    const code = err instanceof FaceError ? err.code : null;
    switch (code) {
      case 'CAMERA_DENIED': return 'Permiso de cámara denegado. Habilítalo en el navegador.';
      case 'CAMERA_UNAVAILABLE': return 'No se pudo acceder a la cámara.';
      case 'MODELS_FAILED': return 'No se pudieron cargar los modelos de reconocimiento facial.';
      case 'NO_FACE': return 'No se detectó un rostro. Mira a la cámara con buena luz.';
      case 'MULTIPLE_FACES': return 'Se detectó más de un rostro. Debe aparecer solo una persona.';
      default: return 'Ocurrió un error con el reconocimiento facial.';
    }
  }
}
