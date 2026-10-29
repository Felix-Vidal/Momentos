package app.momentos.invites.dto;

import app.momentos.groups.GroupRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Criação de convite.
 *
 * @param email        quando informado, o convite é dirigido a essa pessoa; sem ele,
 *                     vira um link que qualquer um com o endereço pode usar
 * @param role         papel com que a pessoa entra
 * @param expiresInHours validade do convite
 * @param maxUses      quantas pessoas podem usar o mesmo link
 */
public record CreateInviteRequest(
        @Email(message = "E-mail inválido.") String email,
        GroupRole role,
        @Min(value = 1, message = "A validade mínima é de 1 hora.")
        @Max(value = 8760, message = "A validade máxima é de um ano.")
        Integer expiresInHours,
        @Min(value = 1, message = "O convite precisa permitir pelo menos um uso.")
        @Max(value = 100, message = "No máximo 100 usos por convite.")
        Integer maxUses
) {
}
