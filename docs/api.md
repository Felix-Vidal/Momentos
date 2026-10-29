# API do Momentos

Base: `/api`. Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`.

## Convenções

- Autenticação por `Authorization: Bearer <access_token>` (JWT de 15 minutos);
  renovação pelo refresh token, que é **rotativo** — renovar revoga o token usado.
- **Arquivos de mídia são a exceção.** Uma tag `<img>`/`<video>` não manda cabeçalho de
  autenticação, então as respostas de login, cadastro e refresh instalam o cookie
  `momentos_media` (`HttpOnly`, `SameSite=Lax`, `Path=/api/media`). Ele carrega um token
  de escopo `media`, que o servidor aceita **somente** em `GET` de `/api/media/{id}/file`
  e `/thumbnail`; em qualquer outra rota o cookie é ignorado e o cabeçalho volta a ser
  obrigatório. O logout o apaga.
- Datas em ISO-8601 com fuso (`2026-10-13T22:00:00-05:00`); o banco guarda `timestamptz`.
- Endpoints que dependem do fuso de quem olha aceitam `timeZone` (ex.: `America/Rio_Branco`).
- Erros seguem sempre o mesmo corpo:

```json
{
  "code": "acesso_negado",
  "message": "Você não tem permissão para isso.",
  "fields": [],
  "timestamp": "2026-10-29T15:40:00Z"
}
```

Códigos: `dados_invalidos`, `nao_encontrado`, `acesso_negado`, `conflito`,
`credenciais_invalidas`, `sessao_invalida`, `nao_autenticado`, `convite_invalido`,
`arquivo_muito_grande`, `erro_interno`.

- Listagens usam página (`page`, `size`); a linha do tempo usa cursor
  (`cursor`, `nextCursor`, `hasMore`), porque cresce pelo fim e é percorrida por rolagem.

### Regras de acesso em uma frase

| Situação | Resposta |
|---|---|
| Não membro em grupo **aberto** | `403` — "Só membros veem o conteúdo deste grupo." |
| Não membro em grupo **privado** | `404` — confirmar a existência já vazaria informação |
| Mídia de terceiro sem grupo em comum | `403` |
| Editar comentário alheio (mesmo admin) | `403` — admin apaga, não edita |

## Autenticação

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cria conta e devolve a sessão |
| POST | `/api/auth/login` | Entra com e-mail e senha |
| POST | `/api/auth/refresh` | Troca o refresh token por uma sessão nova |
| POST | `/api/auth/logout` | Revoga o refresh token |

## Usuários

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/users/me` | Perfil próprio (inclui e-mail) |
| PATCH | `/api/users/me` | Atualiza nome e bio |
| GET | `/api/users/{id}` | Perfil público (sem e-mail) |
| PUT | `/api/users/me/avatar/{mediaId}` | Usa uma mídia própria como foto de perfil |
| DELETE | `/api/users/me/avatar` | Remove a foto de perfil |
| GET | `/api/users/{id}/media?groupId=` | Mídias que a pessoa publicou naquele grupo |

## Grupos

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/groups` | Cria grupo; quem cria vira administrador |
| GET | `/api/groups` | Grupos de que participo |
| GET | `/api/groups/public?q=&page=&size=` | Vitrine pública (só grupos abertos, sem conteúdo) |
| GET | `/api/groups/{groupId}` | Detalhe (só membros) |
| GET | `/api/groups/by-slug/{slug}` | Detalhe pelo slug (só membros) |
| PATCH | `/api/groups/{groupId}` | Nome, descrição, visibilidade, tema (admin) |
| GET | `/api/groups/{groupId}/members` | Integrantes (só membros) |
| PATCH | `/api/groups/{groupId}/members/{userId}` | Promove/rebaixa (admin) |
| DELETE | `/api/groups/{groupId}/members/{userId}` | Remove; o próprio usuário usa para sair |

O grupo não pode ficar sem administrador: o último recebe `409` ao tentar se rebaixar ou sair.

## Mídias

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/media?groupIds=&timeZone=` | Envia uma ou várias (multipart `files`) |
| GET | `/api/media/mine?limit=` | Minhas mídias |
| GET | `/api/media/{id}/file` | Arquivo original |
| GET | `/api/media/{id}/thumbnail` | Miniatura |
| PATCH | `/api/media/{id}` | Corrige data de captura ou legenda (dono) |
| DELETE | `/api/media/{id}` | Apaga a mídia e os arquivos (dono) |
| GET | `/api/media/{id}/groups` | Grupos em que está compartilhada (dono) |
| POST | `/api/media/{id}/groups` | Compartilha com grupos de que participo |
| DELETE | `/api/media/{id}/groups/{groupId}` | Retira do grupo (dono ou admin) |

O envio em lote é tolerante: a resposta traz `uploaded` e `rejected`, com o motivo de cada recusa.

## Linha do tempo e Momentos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/groups/{groupId}/timeline?from=&to=&cursor=&limit=` | Entradas `MOMENT` ou `MEDIA` em ordem cronológica |
| GET | `/api/groups/{groupId}/timeline/day/{data}?timeZone=` | Só as mídias de um dia |
| GET | `/api/groups/{groupId}/timeline/calendar?timeZone=` | Contagem por mês e dia |
| POST | `/api/groups/{groupId}/moments` | Cria um Momento (qualquer membro) |
| GET | `/api/groups/{groupId}/moments` | Momentos do grupo |
| GET | `/api/moments/{momentId}` | Detalhe |
| PATCH | `/api/moments/{momentId}` | Edita (autor ou admin) |
| DELETE | `/api/moments/{momentId}` | Apaga o Momento; as mídias voltam a aparecer soltas |

**Como as mídias caem em um Momento**: o intervalo é meio-aberto `[startsAt, endsAt)`.
Quando Momentos se sobrepõem, vale o de **menor duração** (o mais específico); no empate,
o criado mais recentemente. Momentos sem mídia continuam aparecendo na linha do tempo.

Cada mídia da linha do tempo vem com `commentCount` e uma amostra de `comments`, e cada
bloco de Momento com `commentFeed` — é o que alimenta a caixa animada sem uma chamada por item.

## Comentários

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/media/{mediaId}/comments?groupId=` | Comentários da mídia naquele grupo |
| POST | `/api/media/{mediaId}/comments?groupId=` | Comenta na mídia |
| GET | `/api/moments/{momentId}/comments?limit=` | Feed do conjunto: o Momento e todas as suas mídias |
| POST | `/api/moments/{momentId}/comments` | Comenta no Momento |
| PATCH | `/api/comments/{commentId}` | Edita o próprio texto |
| DELETE | `/api/comments/{commentId}` | Apaga (autor ou admin do grupo) |

Comentário existe **sempre no contexto de um grupo**: a mesma foto compartilhada em dois
grupos tem conversas separadas.

### Tempo real

WebSocket STOMP em `/ws`. O token vai no header `Authorization` do CONNECT, e cada
inscrição em `/topic/groups/{groupId}/comments` é checada contra o quadro de membros.
Eventos: `{ type: CREATED | UPDATED | DELETED, groupId, commentId, mediaId, momentId, comment }`.

## Convites e solicitações

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/groups/{groupId}/invites` | Gera link ou convite dirigido a um e-mail (admin) |
| GET | `/api/groups/{groupId}/invites` | Convites do grupo, sem o token em claro (admin) |
| DELETE | `/api/groups/{groupId}/invites/{inviteId}` | Revoga (admin) |
| GET | `/api/invites/{token}` | Mostra de qual grupo é o convite, antes de aceitar |
| POST | `/api/invites/{token}/accept` | Aceita e entra no grupo |
| GET | `/api/invites/mine` | Convites dirigidos a mim e ainda válidos |
| POST | `/api/groups/{groupId}/join-requests` | Pede para entrar (grupo aberto) |
| GET | `/api/groups/{groupId}/join-requests` | Pedidos pendentes (admin) |
| POST | `/api/groups/{groupId}/join-requests/{id}/approve` | Aprova e coloca no grupo |
| POST | `/api/groups/{groupId}/join-requests/{id}/reject` | Recusa |
| DELETE | `/api/groups/{groupId}/join-requests/mine` | Cancela o próprio pedido |

O token do convite só existe em claro na resposta da criação — o banco guarda apenas o
SHA-256. Convite usado, revogado ou expirado responde `410` com a razão.

## Infraestrutura

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/ping` | Verificação rápida de que a API está no ar |
| GET | `/actuator/health` | Saúde da aplicação |
