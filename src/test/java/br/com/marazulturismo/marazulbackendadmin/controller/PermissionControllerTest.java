package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private String token;

    @BeforeEach
    void setUp() {
        Collaborator admin = userRepository.save(new Collaborator(
                "Admin Teste",
                new Date(),
                Position.DRIVER,
                true, profileRepository.findByName("Administrador").orElseThrow(),
                "admin-permissoes@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/permissoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void list_returnsAllPermissionsWithExpectedFields() throws Exception {
        mockMvc.perform(get("/api/permissoes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].rotaBase").value("dashboard"))
                .andExpect(jsonPath("$[0].funcionalidade").value("view"));
    }

    @Test
    void list_isOrderedByBaseRouteThenFeature() throws Exception {
        mockMvc.perform(get("/api/permissoes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rotaBase").value("dashboard"))
                .andExpect(jsonPath("$[1].rotaBase").value("frota"))
                .andExpect(jsonPath("$[1].funcionalidade").value("create"))
                .andExpect(jsonPath("$[2].funcionalidade").value("delete"))
                .andExpect(jsonPath("$[3].funcionalidade").value("edit"))
                .andExpect(jsonPath("$[4].funcionalidade").value("view"));
    }
}
