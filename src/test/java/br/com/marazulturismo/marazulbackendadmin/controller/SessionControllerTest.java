package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = userRepository.save(new User(
                "Ana Souza",
                new Date(),
                Position.OTHER,
                UserRole.ADMIN,
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
                .andExpect(jsonPath("$").value(aMapWithSize(3)))
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
        User other = userRepository.save(new User(
                "Bruno Lima",
                new Date(),
                Position.OTHER,
                UserRole.ADMIN,
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
}
