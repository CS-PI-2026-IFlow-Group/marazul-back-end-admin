package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ForgotPasswordRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ResetPasswordRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.LoginRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.LoginResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.RegisterRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.service.AuthService;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import br.com.marazulturismo.marazulbackendadmin.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, JwtService jwtService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody @Valid RegisterRequestDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensagem", "Usuário cadastrado com sucesso."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        User user = authService.login(dto);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                user.getId(),
                user.getNome(),
                user.getEmail()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO dto) {
        passwordResetService.forgotPassword(dto.email());
        return ResponseEntity.ok(Map.of("mensagem", "Se o e-mail estiver cadastrado, você receberá as instruções de recuperação."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO dto) {
        passwordResetService.resetPassword(dto.token(), dto.novaSenha());
        return ResponseEntity.ok(Map.of("mensagem", "Senha redefinida com sucesso."));
    }
}
