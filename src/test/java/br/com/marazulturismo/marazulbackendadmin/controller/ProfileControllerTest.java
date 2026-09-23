package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private String token;
    private Profile adminProfile;

    @BeforeEach
    void setUp() {
        adminProfile = profileRepository.findByName("Administrador").orElseThrow();

        Collaborator admin = userRepository.save(new Collaborator(
                "Admin Teste",
                new Date(),
                Position.DRIVER,
                true, adminProfile,
                "admin-perfis@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/perfis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void list_returnsProfilesWithPermissionsInPermissoesFormat() throws Exception {
        mockMvc.perform(get("/api/perfis").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Administrador')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Administrador')].permissions[0].id").exists())
                .andExpect(jsonPath("$[?(@.name=='Administrador')].permissions[0].rotaBase").exists())
                .andExpect(jsonPath("$[?(@.name=='Administrador')].permissions[0].funcionalidade").exists());
    }

    @Test
    void findById_returnsProfileWithPermissions() throws Exception {
        mockMvc.perform(get("/api/perfis/{id}", adminProfile.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminProfile.getId()))
                .andExpect(jsonPath("$.name").value("Administrador"))
                .andExpect(jsonPath("$.permissions[0].id").exists())
                .andExpect(jsonPath("$.permissions[0].rotaBase").exists())
                .andExpect(jsonPath("$.permissions[0].funcionalidade").exists());
    }

    @Test
    void findById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/perfis/{id}", 9999).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void findById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/perfis/{id}", adminProfile.getId()))
                .andExpect(status().isUnauthorized());
    }
}
