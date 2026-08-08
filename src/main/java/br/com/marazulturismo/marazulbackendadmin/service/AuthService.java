package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.LoginRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.RegisterRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.InvalidCredentialsException;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public void register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }

        User user = new User(
                dto.name(),
                new Date(),
                dto.position(),
                dto.userRole(),
                dto.email(),
                null // null para o usuário settar ao receber o email
        );

        userRepository.save(user);

        if (user.getUserRole().equals(UserRole.ADMIN)){
            
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
}
