package app.momentos.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "nao_encontrado", message);
    }

    public static NotFoundException of(String recurso) {
        return new NotFoundException(recurso + " não encontrado.");
    }
}
