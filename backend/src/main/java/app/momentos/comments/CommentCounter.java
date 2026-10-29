package app.momentos.comments;

import app.momentos.moments.Moment;

/**
 * Contagem de comentários de um Momento (os do próprio Momento mais os das suas
 * mídias). Fica numa interface própria para a linha do tempo não depender de todo
 * o módulo de comentários.
 */
public interface CommentCounter {

    long countForMoment(Moment moment);
}
