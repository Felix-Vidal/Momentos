package app.momentos.comments.realtime;

import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Publica os eventos de comentário no tópico do grupo. */
@Component
public class CommentEventPublisher {

    public static final String TOPIC_PREFIX = "/topic/groups/";

    private final SimpMessagingTemplate messaging;

    public CommentEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void publish(UUID groupId, CommentEvent event) {
        messaging.convertAndSend(TOPIC_PREFIX + groupId + "/comments", event);
    }
}
