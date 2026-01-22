package com.ejemplo.vitsync.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {

    // Inyectamos el JavaMailSender para enviar correos electrónicos
    private final JavaMailSender mailSender;

    @Value("${vitsync.email.from}")
    private String fromEmail;

    // Constructor con inyección de dependencias
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void sendHtmlEmail(String destinatary, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true indica que es multipart (necesario para HTML)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(destinatary);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // True indica que es HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            // Manejo de errores básico
            System.err.println("Error al enviar el correo electrónico: " + e.getMessage());
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
                                        <a href="https://vitsync-web-app.vercel.app/login" style="background-color: #0d9488; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold;">Ir a la App</a>
                                    </div>
                                </div>
                                <div style="background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #999999;">
                                    &copy; 2026 Vitsync Team.
                                </div>
                            </div>
                """;
        sendHtmlEmail(destinatary, subject, htmlContent);
    }
}
