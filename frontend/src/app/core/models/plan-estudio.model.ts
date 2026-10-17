export interface PlanEstudio {
  id: number;
  nombre: string;
  descripcion?: string | null;
  decreto?: string | null;
  nivelDesde: number;
  nivelHasta: number;
  activo: boolean;
  cantidadAsignaturas?: number;
}

export interface PlanEstudioDetalle extends PlanEstudio {
  asignaturas: { id: number; nombre: string; codigo?: string }[];
}

export interface PlanEstudioRequest {
  nombre: string;
  descripcion?: string | null;
  decreto?: string | null;
  nivelDesde: number;
  nivelHasta: number;
  activo: boolean;
}
