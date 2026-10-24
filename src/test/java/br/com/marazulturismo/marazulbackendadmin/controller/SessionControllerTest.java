package br.com.marazulturismo.marazulbackendadmin.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private Collaborator admin;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = userRepository.save(new Collaborator(
                "Ana Souza",
                new Date(),
                Position.OTHER,
                true, profileRepository.findByName("Administrador").orElseThrow(),
                "ana.souza@marazul.test",
                null,
                null,
                "$2a$10$hash-irrelevante-para-o-teste"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void me_withValidToken_returnsAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana.souza@marazul.test"));
    }

    @Test
    void me_neverExposesPassword() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(aMapWithSize(5)))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void me_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTokenSignedByAnotherSecret_returns401() throws Exception {
        String foreignToken = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhbmEuc291emFAbWFyYXp1bC50ZXN0IiwiaWQiOjEsInJvbGUiOiJBRE1JTiJ9"
                + ".ZmFrZS1zaWduYXR1cmUtbm90LXZhbGlk";

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenAdminWasDeletedAfterTokenWasIssued_returns404() throws Exception {
        userRepository.deleteById(admin.getId());

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void me_ignoresIdSentAsRequestParameter() throws Exception {
        Collaborator other = userRepository.save(new Collaborator(
                "Bruno Lima",
                new Date(),
                Position.OTHER,
                true, profileRepository.findByName("Administrador").orElseThrow(),
                "bruno.lima@marazul.test",
                null,
                null,
                "$2a$10$outro-hash-irrelevante"));

        mockMvc.perform(get("/api/auth/me")
                        .param("id", String.valueOf(other.getId()))
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value("ana.souza@marazul.test"));
    }
      @Test
    void me_returnsProfileAndPermissionsFromToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("Administrador"))
                .andExpect(jsonPath("$.permissoes", hasItem("clientes:view")));
    }

    @Test
    void me_forProfileWithoutPermissions_returnsEmptyList() throws Exception {
        Profile withoutPermissions = profileRepository.save(new Profile("Sem permissões"));
        Collaborator collaborator = userRepository.save(new Collaborator(
                "Carla Dias",
                new Date(),
                Position.OTHER,
                true, withoutPermissions,
                "carla.dias@marazul.test",
                null,
                null,
                "$2a$10$hash-irrelevante-para-o-teste"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwtService.generateToken(collaborator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("Sem permissões"))
                .andExpect(jsonPath("$.permissoes").isEmpty());
    }

    @Test
    void me_keepsTokenPermissionsWhenProfileChangesAfterLogin() throws Exception {
        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        adminProfile.update("Administrador", List.of());
        profileRepository.saveAndFlush(adminProfile);

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoes", hasItem("clientes:view")));
    }  
}
