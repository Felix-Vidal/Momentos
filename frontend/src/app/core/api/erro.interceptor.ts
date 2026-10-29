import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast.service';

/** Erro da API no formato devolvido pelo GlobalExceptionHandler do backend. */
export interface ErroApi {
  code: string;
  message: string;
  fields?: { field: string; message: string }[];
  timestamp?: string;
}

/**
 * Transforma qualquer falha HTTP em um `ErroApi` e avisa o usuário.
 * Erros de validação (400) seguem adiante sem toast: o formulário é quem mostra.
 */
export const erroInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((erro: HttpErrorResponse) => {
      const corpo: ErroApi = erro.error?.code
        ? erro.error
        : { code: 'falha_rede', message: mensagemPadrao(erro.status) };

      const silencioso = erro.status === 400 || erro.status === 401;
      if (!silencioso) {
        toast.erro(corpo.message);
      }
      return throwError(() => corpo);
    }),
  );
};

function mensagemPadrao(status: number): string {
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
