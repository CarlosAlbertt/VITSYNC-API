package com.ejemplo.vitsync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    //Inyectamos el JavaMailSender para enviar correos electrónicos
    private final JavaMailSender mailSender;

    @Value("${vitsync.email.from}")
    private String fromEmail;

    //Constructor con inyección de dependencias
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String destinatary, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);

        message.setTo(destinatary);
        message.setSubject("Vitsync - Verificación de cuenta");
        message.setText("Hola!,\n\n" +
                "Gracias por registrarte en Vitsync. Para completar el proceso de verificación de tu cuenta, por favor utiliza el siguiente código de verificación:\n\n" +
                "Código de verificación: " + code + "\n\n" +
                "Si no has solicitado esta verificación, por favor ignora este correo electrónico.\n\n" +
                "Gracias por elegirnos,\n\n" +
                "El equipo de Vitsync.");

        mailSender.send(message);
    }

    public void sendWelcomeEmail(String destinatary){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(destinatary);

        message.setSubject("¡Bienvenido a Vitsync!");
        message.setText("¡Tu cuenta ha sido verificada con éxito!\n\n" +
                "Ya puedes acceder a todos los servicios que ofrece Vitsync.\n\n");

        mailSender.send(message);
    }
}
