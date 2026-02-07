package com.cognito.virtual.service;


import com.cognito.virtual.dto.EmailRequest;
import com.cognito.virtual.util.BuildNotificationContent;
import com.cognito.virtual.util.EmailSendingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@org.springframework.stereotype.Service
@lombok.extern.slf4j.Slf4j
public class EmailService {

    private final RestTemplate restTemplate;
    @Autowired
    public BuildNotificationContent notificacionContent;

    @Value("${email.notification-service-url}")
    private String urlApiNotification;

    public EmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendConfirmationEmail(String email, String confirmationCode) {
        log.info("Enviando email de confirmación a: {} con código: {}", email, confirmationCode);
        String subject = "Confirma tu cuenta - Cognito Virtual";
        String content = notificacionContent.buildConfirmationEmailContent(confirmationCode);

        try {
            sendEmailViaNotificationService(email, subject, content);
            log.info("Email de confirmación enviado exitosamente a: {}", email);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación a: {}", email, e);
            throw new EmailSendingException("Error enviando email de confirmación", e);
        }
    }

    public void sendPasswordResetEmail(String email, String resetCode) {
        log.info("Enviando email de reset de password a: {} con código: {}", email, resetCode);
        String subject = "Recuperación de contraseña - Cognito Virtual";
        String content = notificacionContent.buildPasswordResetEmailContent(resetCode);

        try {
            sendEmailViaNotificationService(email, subject, content);
            log.info("Email de reset de password enviado exitosamente a: {}", email);
        } catch (Exception e) {
            log.error("Error enviando email de reset de password a: {}", email, e);
            throw new EmailSendingException("Error enviando email de reset de password", e);
        }
    }

    public void sendPasswordChangeNotification(String email) {
        log.info("Enviando notificación de cambio de password a: {}", email);
        String subject = "Contraseña cambiada - Cognito Virtual";
        String content = notificacionContent.buildPasswordChangeNotificationContent();

        try {
            sendEmailViaNotificationService(email, subject, content);
            log.info("Notificación de cambio de password enviada exitosamente a: {}", email);
        } catch (Exception e) {
            log.error("Error enviando notificación de cambio de password a: {}", email, e);
            throw new EmailSendingException("Error enviando notificación de cambio de password", e);
        }
    }

    private void sendEmailViaNotificationService(String recipient, String subject, String content) {
        EmailRequest request = EmailRequest.builder()
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmailRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    urlApiNotification,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new EmailSendingException("Error en el servicio de notificaciones: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            throw new EmailSendingException("Error conectando con el servicio de notificaciones", e);
        }
    }
}