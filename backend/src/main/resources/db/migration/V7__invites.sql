-- Convites de grupo. Como os refresh tokens, só o hash do token é guardado:
-- o valor em claro existe apenas no link que o administrador compartilha.
create table group_invites (
    id              uuid        primary key default gen_random_uuid(),
    group_id        uuid        not null references groups (id) on delete cascade,
    created_by      uuid        not null references users (id),
    token_hash      varchar(64) not null unique,
    invited_user_id uuid        references users (id) on delete cascade,
    invited_email   varchar(160),
    role            varchar(16) not null,
    expires_at      timestamptz not null,
    max_uses        int         not null default 1,
    uses            int         not null default 0,
    revoked_at      timestamptz,
    created_at      timestamptz not null default now(),
    constraint group_invites_role_check check (role in ('ADMIN', 'MEMBER')),
    constraint group_invites_max_uses_check check (max_uses > 0)
);

create index group_invites_group_idx on group_invites (group_id, created_at desc);
-- Um usuário consulta os convites dirigidos a ele.
create index group_invites_invited_idx on group_invites (invited_user_id)
    where invited_user_id is not null;
