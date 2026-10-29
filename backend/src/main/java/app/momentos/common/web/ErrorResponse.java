package app.momentos.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de erro da API.
 *
 * @param code     identificador estável do erro, para o front tratar sem depender do texto
 * @param message  mensagem em português, exibível ao usuário
 * @param fields   erros por campo, quando o erro vem de validação
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fields,
        Instant timestamp
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String code, String message, List<FieldError> fields) {
        return new ErrorResponse(code, message, fields, Instant.now());
    }
}
