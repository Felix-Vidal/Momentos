# API do Momentos

Base: `/api`. Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`.

## Convenções

- Autenticação por `Authorization: Bearer <access_token>` (JWT de 15 minutos);
  renovação via refresh token.
- Datas em ISO-8601 com fuso (`2026-10-13T22:00:00-05:00`); o banco guarda `timestamptz`.
- Erros seguem sempre o mesmo corpo:

```json
{
  "code": "acesso_negado",
  "message": "Você não tem permissão para isso.",
  "fields": [],
  "timestamp": "2026-10-29T15:40:00Z"
}
```

Códigos usados: `dados_invalidos`, `nao_encontrado`, `acesso_negado`, `conflito`,
`arquivo_muito_grande`, `erro_interno`.

- Listagens comuns usam página (`page`, `size`); a linha do tempo usa cursor
  (`cursor`, `nextCursor`, `hasMore`), porque cresce pelo fim e é percorrida por rolagem.

## Endpoints

### Infraestrutura
| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/ping` | Verificação rápida de que a API está no ar |

As demais rotas são documentadas conforme cada etapa é implementada
(autenticação, grupos, mídias, linha do tempo, Momentos, comentários, convites).
