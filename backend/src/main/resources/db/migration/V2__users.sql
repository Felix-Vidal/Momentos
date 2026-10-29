-- Usuários e sessões.
create table users (
    id              uuid         primary key default gen_random_uuid(),
    name            varchar(80)  not null,
    email           varchar(160) not null,
    password_hash   varchar(100) not null,
    bio             varchar(280),
    avatar_media_id uuid,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);

-- O e-mail identifica o login, então a unicidade ignora caixa.
create unique index users_email_key on users (lower(email));

-- Refresh tokens são guardados apenas como hash: o valor em claro só existe no cliente.
create table refresh_tokens (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null references users (id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create index refresh_tokens_user_idx on refresh_tokens (user_id);
