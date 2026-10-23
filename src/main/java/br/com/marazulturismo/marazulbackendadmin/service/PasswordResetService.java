package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.exception.InvalidOrExpiredTokenException;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final CollaboratorRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.password-reset.base-url}")
    private String resetUrl;

    // Prazo do T04, também informado no texto dos e-mails
    public static final long TOKEN_EXPIRATION_MINUTES = 15;
    private static final long TOKEN_EXPIRATION_MS = TOKEN_EXPIRATION_MINUTES * 60 * 1000;

    public PasswordResetService(CollaboratorRepository userRepository, EmailSenderService emailSenderService) {
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = createResetToken(user);

            sendResetEmail(user, token);

        });
    }

    public void resetPassword(String token, String novaSenha) {
        Collaborator user = userRepository.findByResetToken(token)
                .orElseThrow(InvalidOrExpiredTokenException::new);

        Date expiration = user.getResetTokenExpiration();
        if (expiration == null || !expiration.after(new Date())) {
            user.clearPasswordResetToken();
            userRepository.save(user);
            throw new InvalidOrExpiredTokenException();
        }

        user.updatePasswordHash(passwordEncoder.encode(novaSenha));
        user.clearPasswordResetToken();
        userRepository.save(user);
    }

    private void sendResetEmail(Collaborator user, String token) {
        Context context = new Context();

        String link = buildUrl(token);
        context.setVariable("name", user.getName());
        context.setVariable("link", link);
        context.setVariable("expirationMinutes", TOKEN_EXPIRATION_MINUTES);
        emailSenderService.sendEmailTemplate(user.getEmail(),
            "Recuperação de senha - Marazul Turismo",
            "passwordReset", context);
    }

    public String createResetToken(Collaborator user){
        String token = UUID.randomUUID().toString();
        Date expiration = new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS);

        user.setPasswordResetToken(token, expiration);
        userRepository.save(user);

        return token;
    }

    // TODO: substitiuir pelo endpoint correto com a tela de resetar senha
    private String buildUrl(String token){
        String baseUrl = resetUrl;
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/reset-password?token=" + token;
    }
}
