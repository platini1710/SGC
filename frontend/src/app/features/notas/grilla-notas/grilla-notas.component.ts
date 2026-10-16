import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { CursoService } from '../../../core/services/curso.service';
import { TrimestreService } from '../../../core/services/trimestre.service';
import { NotaService } from '../../../core/services/nota.service';
import { AuthService } from '../../../core/services/auth.service';
import { AnioEscolarService } from '../../../core/services/anio-escolar.service';
import { Curso, CursoAsignatura } from '../../../core/models/curso.model';
import { Trimestre } from '../../../core/models/trimestre.model';
import { GrillaNotas, FilaNota, NotaRequest, AsignacionDocente } from '../../../core/models/nota.model';

@Component({
  selector: 'app-grilla-notas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './grilla-notas.component.html',
  styleUrl: './grilla-notas.component.scss'
})
export class GrillaNotasComponent implements OnInit {
  private cursoService = inject(CursoService);
  private trimestreService = inject(TrimestreService);
  private notaService = inject(NotaService);
  private authService = inject(AuthService);
  private anioEscolarService = inject(AnioEscolarService);

  // true si el usuario autenticado es DOCENTE (usa /mis-asignaciones en vez de cursos globales).
  esDocente = this.authService.getRol() === 'DOCENTE';
  // Asignaciones del docente (cursoId/asignaturaId permitidos). Vacío para ADMIN.
  private misAsignaciones: AsignacionDocente[] = [];

  cursos: Curso[] = [];
  // Solo las asignaturas del curso seleccionado.
  asignaturas: CursoAsignatura[] = [];
  trimestres: Trimestre[] = [];
  // Años disponibles y año seleccionado (el año viaja implícito vía el trimestre elegido).
  anios: number[] = [];
  anio = this.anioEscolarService.anioActual;

  cursoId: number | null = null;
  asignaturaId: number | null = null;
  trimestreId: number | null = null;

  grilla: GrillaNotas | null = null;

  readonly posiciones = [1, 2, 3, 4, 5, 6, 7, 8, 9];

  // Valores en edición y marcas de inválido, indexados por "estudianteId-posicion".
  valores: Record<string, string> = {};
  invalidas: Record<string, boolean> = {};

  // Claves (estudianteId-posicion) que fueron modificadas respecto al valor cargado.
  private modificadas = new Set<string>();
  // Valor original cargado desde el backend, para detectar cambios reales.
  private valoresOriginales: Record<string, string> = {};

  cargando = false;
  grabando = false;
  errorMsg = '';
  mensajeOk = '';

  // 'S' si se debe aproximar el promedio al decimal superior (viene de la tabla parametros).
  private aproxNota = 'N';

  // true mientras sincronizamos valores desde el backend, para ignorar los
  // eventos (ngModelChange) que dispara la reasignación programática de [ngModel].
  private sincronizando = false;

  ngOnInit(): void {
    if (this.esDocente) {
      // DOCENTE: construye los cursos únicos a partir de sus asignaciones.
      this.notaService.getMisAsignaciones().subscribe({
        next: (asignaciones) => {
          this.misAsignaciones = asignaciones;
          const porCurso = new Map<number, string>();
          for (const a of asignaciones) {
            if (!porCurso.has(a.cursoId)) { porCurso.set(a.cursoId, a.nombreCurso); }
          }
          this.cursos = Array.from(porCurso.entries())
            .map(([id, nombre]) => ({ id, nombre, nivel: 0 }));
        },
        error: () => (this.errorMsg = 'Error cargando sus asignaciones')
      });
    } else {
      // ADMIN: flujo original con cursos globales.
      this.cursoService.getAll().subscribe({
        next: (c) => (this.cursos = c),
        error: () => (this.errorMsg = 'Error cargando cursos')
      });
    }
    this.anioEscolarService.anios$.subscribe(a => (this.anios = a));
    this.cargarTrimestres();
    this.notaService.getParametros().subscribe({
      next: (p) => (this.aproxNota = p.aproxNota ?? 'N'),
      error: () => (this.aproxNota = 'N')
    });
  }

  // Carga los trimestres del año seleccionado para poblar el selector de trimestre.
  private cargarTrimestres(): void {
    this.trimestreService.getAll(this.anio).subscribe({
      next: (t) => (this.trimestres = t),
      error: () => (this.errorMsg = 'Error cargando trimestres')
    });
  }

  // Al cambiar el año: recarga los trimestres de ese año y resetea el trimestre/grilla.
  onAnioChange(): void {
    this.anioEscolarService.setAnio(this.anio);
    this.trimestreId = null;
    this.grilla = null;
    this.mensajeOk = '';
    this.errorMsg = '';
    this.cargarTrimestres();
  }

  // Al cambiar el curso: carga solo las asignaturas de ese curso y resetea la selección.
  onCursoChange(): void {
    this.mensajeOk = '';
    this.errorMsg = '';
    this.asignaturaId = null;
    this.asignaturas = [];
    this.grilla = null;
    if (this.cursoId == null) { return; }

    if (this.esDocente) {
      // DOCENTE: filtra las asignaturas desde sus asignaciones, sin llamar al backend.
      this.asignaturas = this.misAsignaciones
        .filter((a) => a.cursoId === this.cursoId)
        .map((a) => ({
          asignaturaId: a.asignaturaId,
          nombreAsignatura: a.nombreAsignatura,
          horasSemanales: 0,
          profesorId: null,
          nombreProfesor: null
        }));
      return;
    }

    this.cursoService.getById(this.cursoId).subscribe({
      next: (detalle) => { this.asignaturas = detalle.asignaturas ?? []; },
      error: () => (this.errorMsg = 'Error cargando las asignaturas del curso')
    });
  }

  get trimestreCerrado(): boolean {
    return !!this.grilla && this.grilla.estadoTrimestre !== 'ABIERTO';
  }

  // El botón Grabar se habilita solo si hay cambios, ninguno es inválido,
  // el trimestre está abierto y no se está grabando ya.
  get puedeGrabar(): boolean {
    return (
      !this.trimestreCerrado &&
      !this.grabando &&
      this.modificadas.size > 0 &&
      !this.hayInvalidas()
    );
  }

  get hayCambios(): boolean {
    return this.modificadas.size > 0;
  }

  // Visible en el template para mostrar el aviso de notas fuera de rango.
  get hayInvalidasVisible(): boolean {
    return this.hayInvalidas();
  }

  private hayInvalidas(): boolean {
    return Object.values(this.invalidas).some((v) => v);
  }

  onSeleccion(): void {
    this.mensajeOk = '';
    this.errorMsg = '';
    if (this.cursoId != null && this.asignaturaId != null && this.trimestreId != null) {
      this.cargarGrilla();
    } else {
      this.grilla = null;
    }
  }

  private cargarGrilla(): void {
    if (this.cursoId == null || this.asignaturaId == null || this.trimestreId == null) { return; }
    this.cargando = true;
    this.notaService.getGrilla(this.cursoId, this.asignaturaId, this.trimestreId).subscribe({
      next: (g) => {
        this.grilla = g;
        this.sincronizarValores(g);
        this.cargando = false;
      },
      error: () => { this.errorMsg = 'Error cargando la grilla de notas'; this.cargando = false; }
    });
  }

  private sincronizarValores(g: GrillaNotas): void {
    this.sincronizando = true;
    this.valores = {};
    this.invalidas = {};
    this.valoresOriginales = {};
    this.modificadas.clear();
    for (const fila of g.filas) {
      for (const nota of fila.notas) {
        const key = this.clave(fila.estudianteId, nota.posicion);
        const txt = this.formatearNumero(nota.valor);
        this.valores[key] = txt;
        this.valoresOriginales[key] = txt;
      }
    }
    // Libera la bandera tras el ciclo de render actual, cuando ya se dispararon
    // los (ngModelChange) de la reasignación programática.
    setTimeout(() => { this.sincronizando = false; }, 0);
  }

  clave(estudianteId: number, posicion: number): string {
    return `${estudianteId}-${posicion}`;
  }

  // Devuelve la clase de color según el valor: rojo si < 4,0, azul si >= 4,0.
  // Cadena vacía / inválido => sin clase.
  claseNota(valorStr: string | undefined): string {
    const texto = (valorStr ?? '').trim().replace(',', '.');
    if (texto === '') { return ''; }
    const valor = Number(texto);
    if (isNaN(valor)) { return ''; }
    return valor < 4.0 ? 'nota-roja' : 'nota-azul';
  }

  // Igual pero para un número ya calculado (promedio).
  claseNotaNum(valor: number | null): string {
    if (valor == null) { return ''; }
    return valor < 4.0 ? 'nota-roja' : 'nota-azul';
  }

  // Promedio en vivo de una fila, calculado desde los valores actuales de la grilla
  // (no desde el backend). Devuelve null si la fila no tiene notas válidas.
  promedioFila(estudianteId: number): number | null {
    const notas: number[] = [];
    for (const p of this.posiciones) {
      const texto = (this.valores[this.clave(estudianteId, p)] ?? '').trim().replace(',', '.');
      if (texto === '') { continue; }
      const valor = Number(texto);
      // Solo cuenta notas numéricas válidas dentro del rango.
      if (!isNaN(valor) && valor >= 1.0 && valor <= 7.0) {
        notas.push(valor);
      }
    }
    if (notas.length === 0) { return null; }
    const suma = notas.reduce((acc, n) => acc + n, 0);
    // Promedio con 2 decimales.
    const prom2 = Math.round((suma / notas.length) * 100) / 100;
    // Aplica la regla de aproximación a 1 decimal según el parámetro aproxNota:
    //  - 'S'  -> redondea al decimal superior (6.35 -> 6.4)
    //  - otro -> trunca el segundo decimal       (6.35 -> 6.3)
    if (this.aproxNota.trim().toUpperCase() === 'S') {
      return Math.round(prom2 * 10) / 10;        // redondeo a 1 decimal
    }
    return Math.floor(prom2 * 10) / 10;          // trunca a 1 decimal
  }

  // Las notas siempre se muestran con dos decimales (ej. 5,3 -> "5.30").
  private formatearNumero(valor: number): string {
    return valor.toFixed(2);
  }

  // Al enfocar una celda, selecciona todo su contenido para que el primer dígito
  // tecleado reemplace el valor anterior (ej. una nota "3.50" ya cargada).
  onFocus(event: FocusEvent): void {
    const input = event.target as HTMLInputElement;
    input.select();
  }

  // id del input de una celda, para poder moverlo por foco.
  inputId(filaIdx: number, colIdx: number): string {
    return `nota-${filaIdx}-${colIdx}`;
  }

  // Mueve el foco a la siguiente celda:
  //  - siguiente columna de la misma fila
  //  - si es la última columna -> primera columna de la fila siguiente
  //  - si es la última fila y última columna -> primera celda (0,0)
  private avanzarFoco(filaIdx: number, colIdx: number): void {
    const totalFilas = this.grilla?.filas.length ?? 0;
    const totalCols = this.posiciones.length; // 9
    if (totalFilas === 0) { return; }

    let nextFila = filaIdx;
    let nextCol = colIdx + 1;
    if (nextCol >= totalCols) {
      nextCol = 0;
      nextFila = filaIdx + 1;
      if (nextFila >= totalFilas) {
        nextFila = 0; // vuelve a la primera fila
      }
    }
    // Espera al siguiente ciclo de render para asegurar que el DOM está listo.
    setTimeout(() => {
      const el = document.getElementById(this.inputId(nextFila, nextCol)) as HTMLInputElement | null;
      if (el) { el.focus(); el.select(); }
    }, 0);
  }

  // Solo se permiten dígitos (sin punto ni coma) y un máximo de 2 dígitos.
  // Ej.: "35" -> 3,5 ; "3" -> 3,0 (la transformación ocurre en onBlur).
  onKeypress(event: KeyboardEvent): void {
    const char = event.key;
    const input = event.target as HTMLInputElement;
    // Rechaza todo lo que no sea dígito (incluye . , y letras).
    if (char < '0' || char > '9') {
      event.preventDefault();
      return;
    }
    // Cuenta solo los dígitos ya presentes (ignora un posible punto de un valor cargado).
    const soloDigitos = input.value.replace(/\D/g, '');
    const seleccion = (input.selectionEnd ?? 0) - (input.selectionStart ?? 0);
    // Si hay texto seleccionado, el dígito lo reemplaza (permitido).
    if (seleccion > 0) { return; }
    // Máximo 2 dígitos.
    if (soloDigitos.length >= 2) {
      event.preventDefault();
    }
  }

  // Mientras el usuario teclea dígitos, solo limpia la marca de inválida.
  // La transformación "35 -> 3.5" y la validación de rango ocurren en onBlur.
  // Al completar el segundo dígito, avanza el foco a la siguiente celda.
  onChange(fila: FilaNota, posicion: number, valorStr: string, filaIdx: number, colIdx: number): void {
    // Ignora los eventos disparados por la sincronización programática desde el backend.
    if (this.sincronizando) { return; }
    // Una edición real del usuario limpia los mensajes anteriores.
    this.mensajeOk = '';
    this.errorMsg = '';
    const key = this.clave(fila.estudianteId, posicion);
    // Durante el tecleo no marcamos inválido (aún no está transformada la nota).
    this.invalidas[key] = false;

    // Autoavance: al completar 2 dígitos salta a la siguiente celda.
    const digitos = (valorStr ?? '').replace(/\D/g, '');
    if (digitos.length >= 2) {
      this.avanzarFoco(filaIdx, colIdx);
    }
  }

  // Compara dos textos como números (5.3 === 5.30). Cadenas vacías se comparan como texto.
  private mismoValor(a: string, b: string): boolean {
    if (a === '' || b === '') { return a === b; }
    const na = Number(a);
    const nb = Number(b);
    if (isNaN(na) || isNaN(nb)) { return a === b; }
    return na === nb;
  }

  // Al salir de la celda, transforma los dígitos escritos en una nota con decimal:
  //   "35" -> 3.5 -> "3.50"  |  "3" -> 3.0 -> "3.00"
  // El primer dígito es el entero y el segundo (si existe) el primer decimal.
  onBlur(fila: FilaNota, posicion: number): void {
    const key = this.clave(fila.estudianteId, posicion);
    const digitos = (this.valores[key] ?? '').trim().replace(/\D/g, ''); // solo dígitos
    if (digitos === '') {
      this.valores[key] = '';
      this.invalidas[key] = false;
      return;
    }

    let valor: number;
    if (digitos.length === 1) {
      valor = Number(digitos);              // "3" -> 3.0
    } else {
      valor = Number(digitos[0] + '.' + digitos[1]); // "35" -> 3.5
    }

    if (isNaN(valor) || valor < 1.0 || valor > 7.0) {
      this.invalidas[key] = true;
      return;
    }

    this.invalidas[key] = false;
    this.valores[key] = valor.toFixed(2);
    // Marca el cambio respecto al original para habilitar el botón Grabar.
    const original = this.valoresOriginales[key] ?? '';
    if (!this.mismoValor(this.valores[key], original)) {
      this.modificadas.add(key);
    } else {
      this.modificadas.delete(key);
    }
  }

  // Graba todas las notas modificadas en lote.
  grabar(): void {
    if (!this.puedeGrabar) { return; }
    if (this.cursoId == null || this.asignaturaId == null || this.trimestreId == null) { return; }

    const peticiones = Array.from(this.modificadas)
      .map((key) => {
        const [estIdStr, posStr] = key.split('-');
        const estudianteId = Number(estIdStr);
        const posicion = Number(posStr);
        const texto = (this.valores[key] ?? '').trim().replace(',', '.');
        if (texto === '') { return null; } // celdas vaciadas no se envían
        const valor = Number(texto);
        const req: NotaRequest = {
          estudianteId,
          asignaturaId: this.asignaturaId as number,
          trimestreId: this.trimestreId as number,
          posicion,
          valor
        };
        return this.notaService.guardar(req);
      })
      .filter((obs): obs is NonNullable<typeof obs> => obs !== null);

    if (peticiones.length === 0) {
      // Solo había celdas vaciadas: nada que grabar.
      this.modificadas.clear();
      this.mensajeOk = 'No hay notas nuevas para grabar.';
      return;
    }

    this.grabando = true;
    this.errorMsg = '';
    forkJoin(peticiones).subscribe({
      next: () => {
        this.grabando = false;
        this.mensajeOk = `Se grabaron ${peticiones.length} nota(s) correctamente.`;
        this.cargarGrilla();
      },
      error: (err: HttpErrorResponse) => {
        this.grabando = false;
        if (err.status === 409) {
          this.errorMsg = err.error?.error ?? 'El trimestre está cerrado';
        } else if (err.status === 400 || err.status === 422) {
          this.errorMsg = err.error?.error ?? 'Hay notas inválidas';
        } else {
          this.errorMsg = 'Error grabando las notas';
        }
      }
    });
  }
}
