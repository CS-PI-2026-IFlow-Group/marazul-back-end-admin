package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.exception.InvalidOrExpiredTokenException;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.base-url}")
    private String resetUrl;

    private static final long TOKEN_EXPIRATION_MS = 15L * 60 * 1000;

    public PasswordResetService(UserRepository userRepository, EmailSenderService emailSenderService) {
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = createResetToken(user);

            sendResetEmail(email, token);

        });
    }

    public void resetPassword(String token, String novaSenha) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(InvalidOrExpiredTokenException::new);

        if (user.getResetTokenExpiration().before(new Date())) {
            throw new InvalidOrExpiredTokenException();
        }

        user.updateSenhaHash(passwordEncoder.encode(novaSenha));
        user.clearPasswordResetToken();
        userRepository.save(user);
    }

    private void sendResetEmail(String to, String token) {
        Context context = new Context();

        String link = buildUrl(token);
        context.setVariable(link, "link");
        emailSenderService.sendEmailTemplate(to, 
            "Recuperação de senha - Marazul Turismo",
            "passwordReset", context);
    }

    public String createResetToken(User user){
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
