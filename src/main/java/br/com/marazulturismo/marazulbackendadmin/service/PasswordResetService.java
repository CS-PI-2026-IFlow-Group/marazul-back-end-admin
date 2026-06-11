package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.exception.InvalidOrExpiredTokenException;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.base-url}")
    private String baseUrl;

    private static final long TOKEN_EXPIRATION_MS = 15L * 60 * 1000;

    public PasswordResetService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            Date expiration = new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS);

            user.setPasswordResetToken(token, expiration);
            userRepository.save(user);

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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Recuperação de Senha - Marazul Turismo");
        message.setText(
                "Você solicitou a recuperação de senha.\n\n" +
                "Clique no link abaixo para redefinir sua senha. O link expirará em 15 minutos.\n\n" +
                baseUrl + "/reset-password?token=" + token + "\n\n" +
                "Se você não solicitou a recuperação de senha, ignore este e-mail."
        );
        mailSender.send(message);
    }
}
