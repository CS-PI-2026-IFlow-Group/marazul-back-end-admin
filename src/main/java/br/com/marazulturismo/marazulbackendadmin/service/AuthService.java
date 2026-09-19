package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.LoginRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.RegisterRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.SessionUserResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.InvalidCredentialsException;
import br.com.marazulturismo.marazulbackendadmin.exception.UserNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Date;

@Service
public class AuthService {

    private final CollaboratorRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailSenderService emailSenderService;
    private final PasswordResetService passwordResetService;
    private final String definePasswordUrl;

    public AuthService(CollaboratorRepository userRepository, ProfileRepository profileRepository,
        EmailSenderService emailSenderService, PasswordResetService passwordResetService, 
        @Value("${app.password-define.base-url}") String definePasswordUrl) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.emailSenderService = emailSenderService;
        this.passwordResetService = passwordResetService;
        this.definePasswordUrl = definePasswordUrl;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }



    public void register(RegisterRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.isUser()) && dto.email() == null) {
            throw new IllegalArgumentException("O e-mail é obrigatório para colaboradores com acesso ao sistema.");
        }
        if (dto.email() != null && userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }

        Position position = dto.position() == Position.DRIVER ? Position.DRIVER : Position.OTHER;
        CNH cnh = dto.cnhNumber() == null && dto.cnhType() == null
                ? null
                : new CNH(dto.cnhNumber(), dto.cnhType());

        Profile profile = profileRepository.findById(dto.profileId())
                .orElseThrow(() -> new IllegalArgumentException("O perfil informado não existe."));
        Collaborator user = new Collaborator(
                dto.name(),
                new Date(),
                position,
                dto.isUser(),
                profile,
                dto.email(),
                dto.cellphoneNumber(),
                cnh,
                null // null para o usuário settar ao receber o email
        );

       
        userRepository.save(user);
        

        if (Boolean.TRUE.equals(user.getIsUser())) {

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

    public SessionUserResponseDTO findSessionUser(Long id) {
        return userRepository.findById(id)
                .map(SessionUserResponseDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Collaborator login(LoginRequestDTO dto) {
        Collaborator user = userRepository.findByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(user.getIsUser())) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(dto.senha(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.registerAccess();
        userRepository.save(user);
        return user;
    }


    public void resendEmail(RegisterRequestDTO dto){
    userRepository.findByEmail(dto.email()).ifPresent(user -> {
        if (Boolean.TRUE.equals(user.getIsUser())) {
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
