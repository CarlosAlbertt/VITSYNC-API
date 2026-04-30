package com.ejemplo.vitsync.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${vitsync.email.from}")
    private String fromEmail;

    private final RestTemplate restTemplate;

    public EmailService() {
        this.restTemplate = new RestTemplate();
    }

    private void sendHtmlEmail(String destinatary, String subject, String htmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromEmail);
            body.put("to", List.of(destinatary));
            body.put("subject", subject);
            body.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject("https://api.resend.com/emails", request, String.class);

            System.out.println("Email enviado exitosamente a: " + destinatary);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo electrónico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendVerificationEmail(String destinatary, String code) {

        String subject = "VitSync - Verificación de cuenta";

        String htmlContent = """
                         <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                                <div style="background-color: #0d9488; padding: 20px; text-align: center;">
                                    <h1 style="color: white; margin: 0; font-size: 24px;">Vitsync</h1>
                                </div>
                                <div style="padding: 30px; background-color: #ffffff;">
                                    <h2 style="color: #333333; margin-top: 0;">¡Hola!</h2>
                                    <p style="color: #555555; line-height: 1.6;">
                                        Gracias por registrarte en Vitsync. Para completar el proceso de verificación, usa el siguiente código:
                                    </p>
                                    <div style="background-color: #f3f4f6; padding: 15px; text-align: center; border-radius: 6px; margin: 20px 0;">
                                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #0d9488;">%s</span>
                                    </div>
                                    <p style="color: #555555; font-size: 14px;">
                                        Si no has solicitado esta verificación, puedes ignorar este correo.
                                    </p>
                                </div>
                                <div style="background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #999999;">
                                    &copy; 2026 Vitsync Team.
                                </div>
                            </div>
                """
                .formatted(code);
        sendHtmlEmail(destinatary, subject, htmlContent);
    }

    public void sendWelcomeEmail(String destinatary) {
        String subject = "¡Bienvenido a VitSync!";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                                <div style="background-color: #0d9488; padding: 20px; text-align: center;">
                                    <h1 style="color: white; margin: 0; font-size: 24px;">Vitsync</h1>
                                </div>
                                <div style="padding: 30px; background-color: #ffffff;">
                                    <h2 style="color: #333333; margin-top: 0;">¡Cuenta Verificada!</h2>
                                    <p style="color: #555555; line-height: 1.6;">
                                        Tu cuenta ha sido verificada con éxito. Ya tienes acceso completo a todos los servicios de Vitsync.
                                    </p>
                                    <div style="text-align: center; margin-top: 30px;">
                                        <a href="https://vitsync.es/login" style="background-color: #0d9488; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold;">Ir a la App</a>
                                    </div>
                                </div>
                                <div style="background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #999999;">
                                    &copy; 2026 Vitsync Team.
                                </div>
                            </div>
                """;
        sendHtmlEmail(destinatary, subject, htmlContent);
    }

    public void send2FACodeEmail(String destinatary, String code) {
        String subject = "VitSync - Código de seguridad 2FA";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                    <div style="background-color: #0d9488; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">Vitsync</h1>
                    </div>
                    <div style="padding: 30px; background-color: #ffffff;">
                        <h2 style="color: #333333; margin-top: 0;">Código de Verificación</h2>
                        <p style="color: #555555; line-height: 1.6;">
                            Alguien está intentando acceder a tu cuenta de Vitsync. Para continuar, introduce el siguiente código de seguridad:
                        </p>
                        <div style="background-color: #f3f4f6; padding: 15px; text-align: center; border-radius: 6px; margin: 20px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #0d9488;">%s</span>
                        </div>
                        <p style="color: #555555; font-size: 14px;">
                            Este código caducará en breve. Si no has sido tú, te recomendamos cambiar tu contraseña inmediatamente.
                        </p>
                    </div>
                    <div style="background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #999999;">
                        &copy; 2026 Vitsync Team.
                    </div>
                </div>
                """.formatted(code);
        sendHtmlEmail(destinatary, subject, htmlContent);
    }
}
