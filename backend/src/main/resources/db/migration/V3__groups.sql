-- Grupos e quadro de membros.
create table groups (
    id              uuid         primary key default gen_random_uuid(),
    slug            varchar(80)  not null unique,
    name            varchar(80)  not null,
    description     varchar(500),
    cover_media_id  uuid,
    visibility      varchar(16)  not null,
    theme           varchar(24)  not null default 'DEFAULT',
    created_by      uuid         not null references users (id),
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now(),
    constraint groups_visibility_check check (visibility in ('OPEN', 'PRIVATE')),
    constraint groups_theme_check check (theme in ('DEFAULT', 'ROULETTE'))
);

-- A vitrine de grupos abertos busca por nome ignorando acentos e caixa.
create index groups_search_idx on groups (momentos_slugify_search(name));
create index groups_visibility_idx on groups (visibility);

create table group_members (
    id        uuid        primary key default gen_random_uuid(),
    group_id  uuid        not null references groups (id) on delete cascade,
    user_id   uuid        not null references users (id) on delete cascade,
    role      varchar(16) not null,
    joined_at timestamptz not null default now(),
    constraint group_members_unique unique (group_id, user_id),
    constraint group_members_role_check check (role in ('ADMIN', 'MEMBER'))
);

create index group_members_user_idx on group_members (user_id);

-- Solicitações de entrada (o fluxo completo de aprovação vem na etapa de convites).
create table join_requests (
    id         uuid        primary key default gen_random_uuid(),
    group_id   uuid        not null references groups (id) on delete cascade,
    user_id    uuid        not null references users (id) on delete cascade,
    status     varchar(16) not null,
    message    varchar(280),
    decided_by uuid        references users (id),
    decided_at timestamptz,
    created_at timestamptz not null default now(),
    constraint join_requests_status_check check (status in ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Uma única solicitação pendente por pessoa e grupo.
create unique index join_requests_pending_key
    on join_requests (group_id, user_id) where status = 'PENDING';
