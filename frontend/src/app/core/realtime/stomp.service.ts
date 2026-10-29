import { inject, Injectable } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Evento de comentário publicado pelo backend no tópico do grupo. */
export interface CommentEvent {
  type: 'CREATED' | 'UPDATED' | 'DELETED';
  groupId: string;
  commentId: string;
  mediaId: string | null;
  momentId: string | null;
  comment: unknown | null;
}

/**
 * Conexão STOMP única, compartilhada por quem precisar escutar um grupo.
 *
 * <p>O token vai no CONNECT (o backend valida ali e confere a participação no grupo
 * em cada inscrição). Se a conexão cair, o cliente reconecta e as inscrições são
 * refeitas — a tela continua funcionando sem tempo real enquanto isso.
 */
@Injectable({ providedIn: 'root' })
export class StompService {
  private readonly auth = inject(AuthService);
  private client?: Client;
  private readonly subscriptions = new Map<string, StompSubscription>();
  private readonly streams = new Map<string, Subject<CommentEvent>>();

  /**
   * Eventos de comentários de um grupo.
   *
   * <p>Quem assina é responsável por cancelar a inscrição — este método é chamado
   * de dentro de um effect, onde `inject()` não está disponível.
   */
  comments(groupId: string): Observable<CommentEvent> {
    const destination = `/topic/groups/${groupId}/comments`;
    let stream = this.streams.get(destination);
    if (!stream) {
      stream = new Subject<CommentEvent>();
      this.streams.set(destination, stream);
    }
    this.ensureConnected();
    this.subscribe(destination);

    return new Observable<CommentEvent>((subscriber) => {
      const inner = stream.subscribe(subscriber);
      return () => {
        inner.unsubscribe();
        this.release(destination);
      };
    });
  }

  private ensureConnected(): void {
    if (this.client) {
      return;
    }
    const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;
    this.client = new Client({
      brokerURL: url,
      connectHeaders: { Authorization: `Bearer ${this.auth.accessToken ?? ''}` },
      reconnectDelay: 4000,
      onConnect: () => {
        // Refaz as inscrições após reconectar.
        this.subscriptions.clear();
        this.streams.forEach((_, destination) => this.subscribe(destination));
      },
    });
    this.client.activate();
  }

  private subscribe(destination: string): void {
    const client = this.client;
    if (!client?.connected || this.subscriptions.has(destination)) {
      return;
    }
    const subscription = client.subscribe(destination, (message) => {
      try {
        this.streams.get(destination)?.next(JSON.parse(message.body) as CommentEvent);
      } catch {
        // Mensagem fora do formato esperado é ignorada: o tempo real é um extra.
      }
    });
    this.subscriptions.set(destination, subscription);
  }

  private release(destination: string): void {
    this.subscriptions.get(destination)?.unsubscribe();
    this.subscriptions.delete(destination);
    this.streams.get(destination)?.complete();
    this.streams.delete(destination);

    if (this.streams.size === 0) {
      void this.client?.deactivate();
      this.client = undefined;
    }
  }
}
