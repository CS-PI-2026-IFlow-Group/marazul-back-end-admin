package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.SessionUserResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.security.AuthenticatedUser;
import br.com.marazulturismo.marazulbackendadmin.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<SessionUserResponseDTO> me(@AuthenticationPrincipal AuthenticatedUser principal, Authentication authentication) {
        List<String> permissoes = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();

        return ResponseEntity.ok(authService.findSessionUser(principal.id(), principal.profile(), permissoes));
    }
}
