package com.accommodationfinder.controller;

import com.accommodationfinder.dto.MessageDtos.SendMessageRequest;
import com.accommodationfinder.security.AppUserDetails;
import com.accommodationfinder.service.MessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP entry point for live chat.
 *
 * <p>Clients send to {@code /app/chat.send}; the service pushes the stored message to
 * both participants on {@code /user/queue/messages}.
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;

    public ChatWebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal) {
        AppUserDetails details = resolve(principal);
        if (details == null) {
            log.debug("Dropping chat frame from an unauthenticated session");
            return;
        }
        messageService.send(request, details.getUser());
    }

    private AppUserDetails resolve(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AppUserDetails details) {
            return details;
        }
        return null;
    }
}
