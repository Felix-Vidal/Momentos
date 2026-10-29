package app.momentos.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.security.GroupAccessGuard;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupAccessGuardTest {

    @Mock
    private GroupRepository groups;

    @Mock
    private GroupMemberRepository members;

    private GroupAccessGuard guard;

    private final UUID groupId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new GroupAccessGuard(groups, members);
    }

    @Test
    void membro_de_grupo_aberto_tem_acesso() {
        aberto();
        when(members.findByGroupIdAndUserId(groupId, userId))
                .thenReturn(Optional.of(new GroupMember(groupId, userId, GroupRole.MEMBER)));

        assertThat(guard.requireMember(groupId, userId).getRole()).isEqualTo(GroupRole.MEMBER);
    }

    @Test
    void grupo_aberto_recusa_quem_nao_e_membro_com_403() {
        aberto();
        when(members.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireMember(groupId, userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Peça para entrar");
    }

    @Test
    void grupo_privado_responde_404_para_quem_nao_e_membro() {
        // Confirmar a existência de um grupo privado já vazaria informação.
        privado();
        when(members.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireMember(groupId, userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void membro_comum_nao_passa_por_requireAdmin() {
        aberto();
        when(members.findByGroupIdAndUserId(groupId, userId))
                .thenReturn(Optional.of(new GroupMember(groupId, userId, GroupRole.MEMBER)));

        assertThatThrownBy(() -> guard.requireAdmin(groupId, userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("administradores");
    }

    @Test
    void administrador_passa_por_requireAdmin() {
        aberto();
        when(members.findByGroupIdAndUserId(groupId, userId))
                .thenReturn(Optional.of(new GroupMember(groupId, userId, GroupRole.ADMIN)));

        assertThat(guard.requireAdmin(groupId, userId).isAdmin()).isTrue();
    }

    @Test
    void grupo_inexistente_responde_404() {
        when(groups.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireGroup(groupId))
                .isInstanceOf(NotFoundException.class);
    }

    private void aberto() {
        when(groups.findById(groupId)).thenReturn(Optional.of(
                new Group("grupo", "Grupo", null, GroupVisibility.OPEN, UUID.randomUUID())));
    }

    private void privado() {
        when(groups.findById(groupId)).thenReturn(Optional.of(
                new Group("grupo", "Grupo", null, GroupVisibility.PRIVATE, UUID.randomUUID())));
    }
}
