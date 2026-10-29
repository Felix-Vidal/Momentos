# Momentos

Linha do tempo colaborativa de fotos e vídeos para grupos de amigos: **Momentos** com título,
comentários animados, convites e grupos abertos ou privados.

O primeiro grupo é a **Sociedade da Roleta** (Bacharelado em Sistemas de Informação da UFAC),
mas a plataforma é genérica: qualquer pessoa cria um grupo.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Security 7 (JWT), Spring Data JPA, Flyway, PostgreSQL 18, WebSocket STOMP |
| Mídia | metadata-extractor (EXIF), Thumbnailator (miniaturas), FFmpeg (vídeo, opcional), MinIO/S3 |
| Frontend | Angular 22 (standalone + signals, zoneless), Tailwind CSS 4, `motion`, `@stomp/stompjs` |
| Testes | JUnit 6, Mockito, Testcontainers 2 (backend); Vitest (frontend) |
| Infra | Docker Compose (PostgreSQL + MinIO + backend) |

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

# 1) infraestrutura
docker compose up -d postgres minio

# 2) backend (http://localhost:8080, Swagger em /swagger-ui.html)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3) frontend (http://localhost:4200, com proxy de /api e /ws para o backend)
cd frontend && npm install && npm start
```

Para subir tudo em container, incluindo o backend: `docker compose up -d`.

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
