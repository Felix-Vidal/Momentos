import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Mensagem de erro de um campo de formulário. */
@Component({
  selector: 'app-field-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (message()) {
      <p class="mt-1.5 text-xs text-danger">{{ message() }}</p>
    }
  `,
})
export class FieldError {
  readonly message = input<string | null | undefined>();
}
