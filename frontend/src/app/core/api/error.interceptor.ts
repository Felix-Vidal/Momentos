import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast.service';

/** Erro da API no formato devolvido pelo GlobalExceptionHandler do backend. */
export interface ApiError {
  code: string;
  message: string;
  fields?: { field: string; message: string }[];
  timestamp?: string;
}

/**
 * Transforma qualquer falha HTTP em um `ApiError` e avisa o usuário.
 * Erros de validação (400) e de sessão (401) seguem adiante sem toast:
 * quem mostra é o formulário ou o fluxo de renovação de token.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const body: ApiError = error.error?.code
        ? error.error
        : { code: 'falha_rede', message: defaultMessage(error.status) };

      if (error.status !== 400 && error.status !== 401) {
        toast.error(body.message);
      }
      return throwError(() => body);
    }),
  );
};

function defaultMessage(status: number): string {
  switch (status) {
    case 0:
      return 'Não foi possível falar com o servidor. Verifique sua conexão.';
    case 403:
      return 'Você não tem permissão para isso.';
    case 404:
      return 'Não encontramos o que você procura.';
    case 413:
      return 'O arquivo é maior que o limite permitido.';
    default:
      return 'Algo deu errado. Tente novamente.';
  }
}
