package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.LoginRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.RegisterRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.InvalidCredentialsException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailSenderService emailSenderService;
    private final PasswordResetService passwordResetService;
    private final String definePasswordUrl;

    public AuthService(UserRepository userRepository, 
        EmailSenderService emailSenderService, PasswordResetService passwordResetService, 
        @Value("${app.password-define.base-url}") String definePasswordUrl) {
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.passwordResetService = passwordResetService;
        this.definePasswordUrl = definePasswordUrl;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }



    public void register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }

        Position position = dto.position() == Position.DRIVER ? Position.DRIVER : Position.OTHER;
        CNH cnh = dto.cnhNumber() == null && dto.cnhType() == null
                ? null
                : new CNH(dto.cnhNumber(), dto.cnhType());

        User user = new User(
                dto.name(),
                new Date(),
                position,
                dto.userRole(),
                dto.email(),
                dto.cellphoneNumber(),
                cnh,
                null // null para o usuário settar ao receber o email
        );

       
        userRepository.save(user);
        

        if (user.getUserRole().equals(UserRole.ADMIN)){

            String token = passwordResetService.createResetToken(user);
            String redifineLink = buildUrl(token);


            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("link", redifineLink);
            emailSenderService.sendEmailTemplate(
            user.getEmail(),
            "Success",
            "newRegister",
            context
        );

        }

    }

    public User login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(dto.senha(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }


    public void resendEmail(RegisterRequestDTO dto){
    userRepository.findByEmail(dto.email()).ifPresent(user -> {
        if (user.getUserRole().equals(UserRole.ADMIN)) {
            String token = passwordResetService.createResetToken(user);
            String redefineLink = buildUrl(token);

            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("link", redefineLink);

            emailSenderService.sendEmailTemplate(
                    user.getEmail(),
                    "Success",
                    "newRegister",
                    context
            );
        }
    });
    }

// TODO: substitiuir pelo endpoint correto com a tela de definir senha
    private String buildUrl(String token){
        String baseUrl = definePasswordUrl;
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/define-password?token=" + token;
    }
}
