package br.com.marazulturismo.marazulbackendadmin.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;


    public void sendEmail(String recipient, String subject, String message){
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }


    @Async
    public void sendEmailTemplate(String recipient, String subject, String template, Context context){
        String templateString = templateEngine.process(template, context);
        MimeMessage message = mailSender.createMimeMessage();
        try{
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            messageHelper.setTo(recipient);
            messageHelper.setSubject(subject);
            messageHelper.setText(templateString, true);
            mailSender.send(message);
        }catch (MessagingException e){
            e.printStackTrace();
        }
    }
    
}
