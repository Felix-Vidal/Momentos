package app.momentos.common.config;

import app.momentos.security.CurrentUser;
import app.momentos.security.GroupAccessGuard;
import app.momentos.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP para os comentários em tempo real.
 *
 * <p>A conexão carrega o mesmo access token da API, e cada inscrição em
 * {@code /topic/groups/{id}/comments} é checada contra o quadro de membros: sem isso,
 * qualquer pessoa autenticada poderia escutar a conversa de um grupo alheio.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String TOPIC_PREFIX = "/topic/groups/";

    private final JwtService jwtService;
    private final GroupAccessGuard access;

    public WebSocketConfig(JwtService jwtService, GroupAccessGuard access) {
        this.jwtService = jwtService;
        this.access = access;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorize(accessor);
                }
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        List<String> header = accessor.getNativeHeader("Authorization");
        String token = header == null || header.isEmpty() ? null : header.getFirst();
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Conexão sem token.");
        }
        CurrentUser user = jwtService.readAccessToken(token.substring(7))
                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));
        accessor.setUser(new StompPrincipal(user));
    }

    private void authorize(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            throw new IllegalArgumentException("Destino não permitido.");
        }
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new IllegalArgumentException("Inscrição sem sessão.");
        }
        String groupPart = destination.substring(TOPIC_PREFIX.length()).split("/")[0];
        UUID groupId;
        try {
            groupId = UUID.fromString(groupPart);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Grupo inválido no destino.");
        }
        if (!access.isMember(groupId, principal.user().id())) {
            throw new IllegalArgumentException("Você não participa deste grupo.");
        }
    }

    /** Identidade associada à sessão STOMP. */
    record StompPrincipal(CurrentUser user) implements java.security.Principal {

        @Override
        public String getName() {
            return user.id().toString();
        }
    }
}
