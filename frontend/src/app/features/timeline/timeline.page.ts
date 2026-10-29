import { ChangeDetectionStrategy, Component } from '@angular/core';
import { EmptyState } from '../../shared/ui/empty-state';

/** Placeholder: a linha do tempo é construída na etapa de Momentos. */
@Component({
  selector: 'app-timeline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  template: `
    <app-empty-state
      icon="◷"
      title="A linha do tempo aparece aqui"
      hint="Envie as primeiras fotos e vídeos para começar a contar a história do grupo."
    />
  `,
})
export class TimelinePage {}
