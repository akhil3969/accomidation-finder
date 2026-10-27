package com.accommodationfinder.service;

import com.accommodationfinder.model.Notification;
import com.accommodationfinder.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/** Writes the emails. {@link MailGateway} delivers them. Plain text: it renders everywhere and never lands in spam for its markup. */
@Service
public class EmailService {

    private static final String SIGN_OFF = "\n\n- The AccomFinder team\n";

    private final MailGateway mailGateway;
    private final String frontendUrl;

    public EmailService(MailGateway mailGateway, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailGateway = mailGateway;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void sendVerification(User user, String rawToken) {
        mailGateway.send(user.getEmail(), "Confirm your email address",
                "Hi " + firstName(user) + ",\n\n"
                + "Please confirm this is your email address so you can book rooms, message\n"
                + "landlords and publish listings:\n\n"
                + frontendUrl + "/verify-email?token=" + rawToken + "\n\n"
                + "The link works for 48 hours. If you did not create an account, ignore this email."
                + SIGN_OFF);
    }

    public void sendPasswordReset(User user, String rawToken) {
        mailGateway.send(user.getEmail(), "Reset your password",
                "Hi " + firstName(user) + ",\n\n"
                + "Someone - hopefully you - asked to reset the password for this account.\n"
                + "Choose a new one here:\n\n"
                + frontendUrl + "/reset-password?token=" + rawToken + "\n\n"
                + "The link works once, for 1 hour. If it was not you, ignore this email:\n"
                + "your password has not changed."
                + SIGN_OFF);
    }

    public void sendPasswordChanged(User user) {
        mailGateway.send(user.getEmail(), "Your password was changed",
                "Hi " + firstName(user) + ",\n\n"
                + "The password for your AccomFinder account was just changed, and every device\n"
                + "that was signed in has been signed out.\n\n"
                + "If this was not you, reset it straight away: " + frontendUrl + "/forgot-password"
                + SIGN_OFF);
    }

    public void sendAccountDeleted(String email, String name) {
        mailGateway.send(email, "Your account has been deleted",
                "Hi " + firstNameOf(name) + ",\n\n"
                + "Your AccomFinder account and the personal data attached to it have been erased,\n"
                + "as you asked. Booking records your landlord or tenant needs to keep now show\n"
                + "\"Deleted user\" instead of your name.\n\n"
                + "You do not need to do anything else."
                + SIGN_OFF);
    }

    /** One email listing everything the person missed, rather than one email per event. */
    public void sendNotificationDigest(User user, List<Notification> notifications) {
        StringBuilder body = new StringBuilder("Hi " + firstName(user) + ",\n\nWhile you were away:\n\n");
        for (Notification n : notifications) {
            body.append("- ").append(n.getTitle());
            if (n.getBody() != null && !n.getBody().isBlank()) {
                body.append("\n  ").append(n.getBody());
            }
            if (n.getLink() != null) {
                body.append("\n  ").append(frontendUrl).append(n.getLink());
            }
            body.append("\n\n");
        }
        body.append("You can turn these emails off on your profile page: ").append(frontendUrl).append("/profile");
        body.append(SIGN_OFF);

        String subject = notifications.size() == 1
                ? notifications.get(0).getTitle()
                : "You have " + notifications.size() + " updates on AccomFinder";
        mailGateway.send(user.getEmail(), subject, body.toString());
    }

    private static String firstName(User user) {
        return firstNameOf(user.getName());
    }

    private static String firstNameOf(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+")[0];
    }
}
