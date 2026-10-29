# Momentos

Linha do tempo colaborativa de fotos e vídeos para grupos de amigos: **Momentos** com título,
comentários animados, convites e grupos abertos ou privados.

O primeiro grupo é a **Sociedade da Roleta** (Bacharelado em Sistemas de Informação da UFAC),
mas a plataforma é genérica: qualquer pessoa cria um grupo.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Security 7 (JWT), Spring Data JPA, Flyway, PostgreSQL 18, WebSocket STOMP |
| Mídia | metadata-extractor (EXIF), Thumbnailator (miniaturas), FFmpeg (vídeo, opcional), disco local ou S3 (AWS SDK v2) |
| Frontend | Angular 22 (standalone + signals, zoneless), Tailwind CSS 4, `motion`, `@stomp/stompjs` |
| Testes | JUnit 6, Mockito, Testcontainers 2 (backend); Vitest (frontend) |
| Infra | Docker Compose (PostgreSQL + backend) |

## Pré-requisitos

- **Java 25** (`java -version`)
- **Node 24+** (recomendado 26 LTS: `nvm install 26`)
- **Docker** + Compose v2 — `sudo apt install docker.io docker-compose-v2` e
  `sudo usermod -aG docker $USER` (relogar depois)
- **FFmpeg** (opcional): com ele os vídeos são comprimidos e ganham miniatura; sem ele o
  arquivo é guardado como está e o player usa um poster padrão

## Como rodar

```bash
cp .env.example .env                 # ajuste se quiser

# 1) infraestrutura (o PostgreSQL do container escuta na 5433,
#    para não conflitar com um PostgreSQL instalado na máquina)
docker compose up -d postgres

# 2) backend (http://localhost:8080, Swagger em /swagger-ui.html)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3) frontend (http://localhost:4200, com proxy de /api e /ws para o backend)
cd frontend && npm install && npm start
```

Para subir tudo em container, incluindo o backend: `docker compose up -d`.

### Dados de demonstração

No perfil `dev`, o seed cria a **Sociedade da Roleta** na primeira subida (e não repete
depois): 8 integrantes, ~28 fotos e 3 vídeos gerados localmente, 5 Momentos, 30
comentários, um pedido de entrada aguardando aprovação e um segundo grupo privado para
comparar visibilidades.

Os Momentos foram escolhidos para mostrar cada regra da linha do tempo:

| Momento | Intervalo | Para ver |
|---|---|---|
| **Corujão da LetalCompany** | 13/10 22:00 → 14/10 06:00 | intervalo que atravessa a meia-noite (10 mídias, 2 vídeos) |
| **Almoço no RU** | 13/10 11:30 → 14:00 | período do dia (4 mídias, 1 vídeo) |
| **Semana de provas** | 12/10 → 17/10 | sobreposição: contém os dois acima, mas fica só com as mídias que eles não levaram |
| **Sábado de maratona** | 18/10, dia inteiro | Momento de dia inteiro |
| **Churrasco de fim de semestre** | 24/10 12:00 → 20:00 | Momento **sem nenhuma mídia**: o título existe e aceita comentários |

Os vídeos (gerados com ffmpeg) exercitam o player, o selo de duração e a miniatura
extraída de um quadro. Sem ffmpeg na máquina, o seed simplesmente não cria vídeos.

Entre com qualquer um destes e a senha `momentos123`:

| Conta | Papel |
|---|---|
| `felix@ufac.br` | administrador |
| `ana@ufac.br` | administradora |
| `bruno@ufac.br`, `carla@ufac.br`, `diego@ufac.br`, `eduarda@ufac.br`, `gustavo@ufac.br` | integrantes |
| `helena@ufac.br` | fora do grupo, com pedido pendente |

Para recomeçar do zero: `docker compose down -v` e apague `backend/storage/`.

### O que olhar primeiro

1. Abra a Sociedade da Roleta e veja a roleta girando no topo.
2. Role a linha do tempo: as mídias de 13–14/10 aparecem sob "Corujão da LetalCompany".
3. **Passe o mouse em uma foto** — a caixa de vidro sobe na base e percorre os comentários.
4. **Passe o mouse no título do Momento** — a mesma caixa percorre a conversa inteira do
   conjunto, com a miniatura da foto de cada comentário.
5. Clique para abrir o lightbox com o texto completo e comentar.
6. No celular (ou DevTools em viewport estreita): o primeiro toque mostra a caixa, o
   segundo abre o lightbox.

## Testes

```bash
cd backend && ./mvnw test      # unitários
cd backend && ./mvnw verify    # + testes de integração (*IT) com PostgreSQL real
                               # sem Docker, os *IT são ignorados em vez de falharem
cd frontend && npm test        # Vitest
```

## Estrutura

```
backend/    Spring Boot, pacotes por funcionalidade (auth, users, groups, media,
            moments, timeline, comments, invites, storage, seed)
frontend/   Angular (core/ infraestrutura, shared/ UI reutilizável, features/ telas)
docs/       documentação de apoio da API
```

## Conceitos

- **Mídia** pertence ao usuário que enviou e é compartilhada com um ou mais grupos.
- **Momento** é um intervalo de data e hora com título (ex.: *Corujão da LetalCompany*,
  13/10 22:00 → 14/10 06:00): toda mídia capturada dentro do intervalo aparece agrupada sob ele.
- **Comentário** existe sempre no contexto de um grupo e aponta para uma mídia ou para um Momento.
