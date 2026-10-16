// Modelos del flujo de promoción masiva, espejo de los DTOs del backend.

// Acciones posibles en la confirmación de promoción.
export type AccionPromocion = 'PROMOVER' | 'REPITENTE' | 'EXCLUIR' | 'EGRESA';

export interface PromocionPreviewRequest {
  anioOrigen: number;
  anioDestino: number;
}

export interface PromocionItemDTO {
  estudianteId: number;
  nombreEstudiante: string;
  cursoOrigenId: number;
  cursoOrigenNombre: string;
  nivelOrigen: number;
  // PROMOVER | EGRESA | SIN_CURSO_DESTINO
  accionPropuesta: string;
  cursoDestinoId: number | null;
  cursoDestinoNombre: string | null;
  yaExisteEnDestino: boolean;
}

export interface PromocionPreviewResponse {
  anioOrigen: number;
  anioDestino: number;
  items: PromocionItemDTO[];
  incidencias: string[];
}

export interface PromocionDecisionDTO {
  estudianteId: number;
  accion: string;
  cursoDestinoId?: number | null;
}

export interface PromocionConfirmRequest {
  anioOrigen: number;
  anioDestino: number;
  decisiones: PromocionDecisionDTO[];
}

export interface PromocionResultadoResponse {
  anioOrigen: number;
  anioDestino: number;
  promovidos: number;
  repitentes: number;
  egresados: number;
  excluidos: number;
  actualizados: number;
  incidencias: string[];
}
