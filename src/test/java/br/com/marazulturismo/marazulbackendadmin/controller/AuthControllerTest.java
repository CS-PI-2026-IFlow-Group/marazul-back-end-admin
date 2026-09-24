package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String PASSWORD = "Senha@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Profile adminProfile;

    @BeforeEach
    void setUp() {
        collaboratorRepository.deleteAll();
        adminProfile = profileRepository.findByName("Administrador").orElseThrow();
    }

    @Test
    void login_forActiveSystemUser_recordsLastAccessAndReturnsProfilePermissions() throws Exception {
        Collaborator collaborator = saveCollaborator("acesso@marazul.test", true, adminProfile);
        LocalDateTime beforeLogin = LocalDateTime.now();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"acesso@marazul.test","senha":"Senha@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = responseToken(result);
        Claims claims = jwtService.parseToken(token);
        entityManager.flush();
        entityManager.clear();

        Collaborator persisted = collaboratorRepository.findById(collaborator.getId()).orElseThrow();
        assertThat(persisted.getLastAccess()).isNotNull().isAfterOrEqualTo(beforeLogin);
        assertThat(claims.get("profile", String.class)).isEqualTo("Administrador");
        assertThat(claims.get("permissions", List.class))
                .contains("dashboard:view", "funcionario:create", "frota:delete");
    }

    @Test
    void login_forCollaboratorWithoutSystemAccess_returns401() throws Exception {
        saveCollaborator("sem-acesso@marazul.test", false, adminProfile);

        mockMvc.perform(loginRequest("sem-acesso@marazul.test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_forInactiveCollaborator_returns401() throws Exception {
        Collaborator collaborator = saveCollaborator("inativo@marazul.test", true, adminProfile);
        collaborator.deactivate();
        collaboratorRepository.save(collaborator);

        mockMvc.perform(loginRequest("inativo@marazul.test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_rejectsTokenWithoutRequiredProfilePermission() throws Exception {
        Profile withoutPermissions = profileRepository.save(new Profile("Sem permissões"));
        Collaborator collaborator = saveCollaborator("sem-permissao@marazul.test", true, withoutPermissions);
        String token = jwtService.generateToken(collaborator);

        mockMvc.perform(get("/api/dashboard/metricas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private Collaborator saveCollaborator(String email, boolean isUser, Profile profile) {
        return collaboratorRepository.save(new Collaborator(
                "Colaborador de teste",
                new Date(),
                Position.OTHER,
                isUser,
                profile,
                email,
                null,
                null,
                passwordEncoder.encode(PASSWORD)));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(String email) {
        return post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + PASSWORD + "\"}");
    }

    private String responseToken(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
}
