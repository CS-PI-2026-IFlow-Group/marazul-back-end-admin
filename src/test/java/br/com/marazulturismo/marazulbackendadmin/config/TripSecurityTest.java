package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprova que um perfil sem as permissões de "viagens" recebe recusa (403 Forbidden)
 * ao tentar acessar os endpoints /api/viagens/**, conforme exigido pela Definição de Pronto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    /** Token de um usuário cujo perfil NÃO tem permissões de viagens. */
    private String tokenSemPermissaoViagens;

    /** Token do admin (com todas as permissões). */
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        // Perfil sem nenhuma permissão de viagens (apenas frota:view como exemplo)
        Profile perfilLimitado = profileRepository.save(new Profile("Perfil Limitado"));
        List<Permission> permissoesFrota = permissionRepository.findAll().stream()
                .filter(p -> p.getBaseRoute().equals("frota"))
                .toList();
        perfilLimitado.grantPermissions(permissoesFrota);

        Collaborator userLimitado = collaboratorRepository.save(new Collaborator(
                "Usuário Limitado",
                new Date(),
                Position.OTHER,
                true,
                perfilLimitado,
                "limitado@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        tokenSemPermissaoViagens = "Bearer " + jwtService.generateToken(userLimitado);

        // Token admin com todas as permissões
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Segurança",
                new Date(),
                Position.DRIVER,
                true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.seguranca@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        tokenAdmin = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void get_viagens_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/viagens")
                        .header("Authorization", tokenSemPermissaoViagens))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_viagens_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/viagens")
                        .header("Authorization", tokenSemPermissaoViagens)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void put_viagens_withoutPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/viagens/{id}", 1)
                        .header("Authorization", tokenSemPermissaoViagens)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_viagens_withoutPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/viagens/{id}", 1)
                        .header("Authorization", tokenSemPermissaoViagens))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_viagens_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/viagens"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_viagens_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
