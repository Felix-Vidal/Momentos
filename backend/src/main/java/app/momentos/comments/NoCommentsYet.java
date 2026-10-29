package app.momentos.comments;

import app.momentos.moments.Moment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enquanto o módulo de comentários não está ativo, a linha do tempo responde
 * com contagem zero em vez de falhar.
 */
@Configuration
public class NoCommentsYet {

    @Bean
    @ConditionalOnMissingBean(CommentCounter.class)
    public CommentCounter noCommentCounter() {
        return (Moment moment) -> 0L;
    }
}
