package app.momentos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe seu nome.")
        @Size(min = 2, max = 80, message = "O nome deve ter de 2 a 80 caracteres.")
        String name,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Escolha uma senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String password
) {
}
