package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Optional;

/**
 * Ponto único de geração do token e envio do e-mail de definição de senha,
 * usado pelo cadastro do painel, pelo registro e pelo reenvio.
 */
@Service
public class PasswordSetupEmailService {

    public static final String SEND_FAILURE_WARNING =
            "Não foi possível enviar o e-mail de definição de senha. Tente reenviar mais tarde.";

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupEmailService.class);

    private final PasswordResetService passwordResetService;
    private final EmailSenderService emailSenderService;
    private final String definePasswordUrl;

    public PasswordSetupEmailService(PasswordResetService passwordResetService,
            EmailSenderService emailSenderService,
            @Value("${app.password-define.base-url}") String definePasswordUrl) {
        this.passwordResetService = passwordResetService;
        this.emailSenderService = emailSenderService;
        this.definePasswordUrl = definePasswordUrl;
    }

    /**
     * Falha no servidor de e-mail não propaga: fica no log e volta como aviso,
     * sem desfazer o cadastro. Optional vazio significa e-mail enviado.
     */
    public Optional<String> send(Collaborator collaborator) {
        String token = passwordResetService.createResetToken(collaborator);

        Context context = new Context();
        context.setVariable("name", collaborator.getName());
        context.setVariable("link", buildUrl(token));
        context.setVariable("expirationMinutes", PasswordResetService.TOKEN_EXPIRATION_MINUTES);

        try {
            emailSenderService.sendEmailTemplateSync(
                    collaborator.getEmail(),
                    "Defina sua senha - Marazul Turismo",
                    "newRegister",
                    context
            );
            return Optional.empty();
        } catch (MailException e) {
            log.error("Falha ao enviar o e-mail de definição de senha do colaborador {}.", collaborator.getId(), e);
            return Optional.of(SEND_FAILURE_WARNING);
        }
    }

    private String buildUrl(String token) {
        String baseUrl = definePasswordUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/define-password?token=" + token;
    }
}
