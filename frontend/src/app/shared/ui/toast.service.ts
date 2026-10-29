import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 0;
  private readonly items = signal<Toast[]>([]);

  readonly toasts = this.items.asReadonly();

  success(text: string): void {
    this.show('success', text);
  }

  error(text: string): void {
    this.show('error', text);
  }

  info(text: string): void {
    this.show('info', text);
  }

  dismiss(id: number): void {
    this.items.update((current) => current.filter((t) => t.id !== id));
  }

  private show(kind: ToastKind, text: string): void {
    const id = this.nextId++;
    this.items.update((current) => [...current, { id, kind, text }]);
    setTimeout(() => this.dismiss(id), 5000);
  }
}
