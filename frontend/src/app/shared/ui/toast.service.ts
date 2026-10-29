import { Injectable, signal } from '@angular/core';

export type TipoToast = 'sucesso' | 'erro' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  texto: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private proximoId = 0;
  private readonly lista = signal<Toast[]>([]);

  readonly toasts = this.lista.asReadonly();

  sucesso(texto: string): void {
    this.mostrar('sucesso', texto);
  }

  erro(texto: string): void {
    this.mostrar('erro', texto);
  }

  info(texto: string): void {
    this.mostrar('info', texto);
  }

  fechar(id: number): void {
    this.lista.update((atuais) => atuais.filter((t) => t.id !== id));
  }

  private mostrar(tipo: TipoToast, texto: string): void {
    const id = this.proximoId++;
    this.lista.update((atuais) => [...atuais, { id, tipo, texto }]);
    setTimeout(() => this.fechar(id), 5000);
  }
}
