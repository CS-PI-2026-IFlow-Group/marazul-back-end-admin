package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.LoginRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.RegisterRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.SessionUserResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeValidationException;
import br.com.marazulturismo.marazulbackendadmin.exception.InvalidCredentialsException;
import br.com.marazulturismo.marazulbackendadmin.exception.UserNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final CollaboratorRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordSetupEmailService passwordSetupEmailService;

    public AuthService(CollaboratorRepository userRepository, ProfileRepository profileRepository,
        PasswordSetupEmailService passwordSetupEmailService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordSetupEmailService = passwordSetupEmailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }



    // Devolve o aviso de falha no envio do e-mail, se houver
    public Optional<String> register(RegisterRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.isUser()) && dto.email() == null) {
            throw new EmployeeValidationException("O e-mail é obrigatório para colaboradores com acesso ao sistema.");
        }
        if (dto.email() != null && userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }

        Position position = dto.position() == Position.DRIVER ? Position.DRIVER : Position.OTHER;
        CNH cnh = buildCnh(dto);

        Profile profile = profileRepository.findById(dto.profileId())
                .orElseThrow(() -> new EmployeeValidationException("O perfil informado não existe."));
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
            return passwordSetupEmailService.send(user);
        }
        return Optional.empty();
    }

    public SessionUserResponseDTO findSessionUser(Long id, String perfil, List<String> permissoes) {
        return userRepository.findById(id)
                .map(user -> SessionUserResponseDTO.fromEntity(user, perfil, permissoes))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Collaborator login(LoginRequestDTO dto) {
        Collaborator user = userRepository.findWithProfileAndPermissionsByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(user.getIsUser()) || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(dto.senha(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.registerAccess();
        userRepository.save(user);
        return user;
    }

    private static CNH buildCnh(RegisterRequestDTO dto) {
        if (dto.position() != Position.DRIVER) {
            return null;
        }

        if (dto.cnhNumber() == null || dto.cnhNumber().isBlank() || dto.cnhType() == null) {
            throw new EmployeeValidationException(
                    "CNH e categoria da CNH são obrigatórias para funcionários motoristas.");
        }

        return new CNH(dto.cnhNumber().trim(), dto.cnhType());
    }
}
