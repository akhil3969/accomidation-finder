package com.accommodationfinder.config;

import com.accommodationfinder.security.AppUserDetails;
import com.accommodationfinder.security.AppUserDetailsService;
import com.accommodationfinder.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame from the {@code Authorization} header,
 * so private queues ({@code /user/queue/**}) can be routed to the right person.
 * Public topics stay readable by anonymous visitors.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public WebSocketAuthChannelInterceptor(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = firstHeader(accessor, "Authorization");
        if (token == null) {
            token = firstHeader(accessor, "token");
        }
        if (token == null) {
            return message;   // anonymous - can still subscribe to public topics
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        try {
            String email = jwtService.extractUsername(token);
            AppUserDetails details = (AppUserDetails) userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(token, details.getUsername())) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                accessor.setUser(authentication);
            }
        } catch (Exception ex) {
            log.debug("STOMP CONNECT with invalid token: {}", ex.getMessage());
        }
        return message;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
