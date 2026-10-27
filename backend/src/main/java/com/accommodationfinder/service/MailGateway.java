package com.accommodationfinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * The only class that talks to SMTP.
 *
 * Spring Boot creates a {@link JavaMailSender} only when {@code spring.mail.host} is set.
 * Without one (demo, local dev) the email is written to the log instead, so the
 * verification and reset links can still be clicked from the console. The prod profile
 * turns body logging off, because those links are credentials.
 *
 * {@link Async}: an SMTP round trip takes a second or two and can time out; neither
 * should hold up the HTTP request that caused the email.
 */
@Component
public class MailGateway {

    private static final Logger log = LoggerFactory.getLogger(MailGateway.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean logBody;

    public MailGateway(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.mail.log-body:true}") boolean logBody) {
        this.mailSender = mailSender;
        this.from = from;
        this.logBody = logBody;
    }

    @Async
    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            if (logBody) {
                log.info("No SMTP configured - email not sent.\nTo: {}\nSubject: {}\n\n{}", to, subject, body);
            } else {
                log.warn("No SMTP configured - email to {} ({}) was dropped", to, subject);
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException ex) {
            // Never surfaces to the user: the action that triggered the email already
            // succeeded. "Resend" on the verify banner and "forgot password" both retry.
            log.error("Could not send '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
