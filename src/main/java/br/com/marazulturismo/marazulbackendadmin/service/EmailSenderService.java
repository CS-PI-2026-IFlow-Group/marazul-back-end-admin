package br.com.marazulturismo.marazulbackendadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
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

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

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
        try {
            sendEmailTemplateSync(recipient, subject, template, context);
        } catch (MailException e) {
            log.error("Falha ao enviar o e-mail '{}' para {}.", template, recipient, e);
        }
    }

    // Envia na mesma thread e propaga a falha, para quem precisa avisar o usuário
    public void sendEmailTemplateSync(String recipient, String subject, String template, Context context) {
        String templateString = templateEngine.process(template, context);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
            messageHelper.setTo(recipient);
            messageHelper.setSubject(subject);
            messageHelper.setText(templateString, true);
        } catch (MessagingException e) {
            throw new MailPreparationException("Não foi possível montar o e-mail.", e);
        }
        mailSender.send(message);
    }

}
