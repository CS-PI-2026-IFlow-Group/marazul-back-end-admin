package br.com.marazulturismo.marazulbackendadmin.controller;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private String token;
    private List<Permission> permissions;

    @BeforeEach
    void setUp() {
        permissions = permissionRepository.findAll();
        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Teste",
                new Date(),
                Position.OTHER,
                true,
                adminProfile,
                "profiles-admin-" + System.nanoTime() + "@marazul.test",
                null,
                null,
                "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void allEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/perfis"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/perfis/{id}", 999L))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/perfis").contentType(MediaType.APPLICATION_JSON).content(validPayload("Operacao")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/perfis/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Operacao")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/perfis/{id}", 999L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsProfilesWithIdsNamesAndPermissions() throws Exception {
        Profile profile = createProfile("Operacao", permissions.subList(0, 2));

        mockMvc.perform(get("/api/perfis").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItems("Administrador", "Operacao")))
                .andExpect(jsonPath("$[?(@.id == %d)].permissions", profile.getId()).isArray());
    }

    @Test
    void findById_returnsProfileAndPermissions() throws Exception {
        Profile profile = createProfile("Comercial", permissions.subList(0, 2));

        mockMvc.perform(get("/api/perfis/{id}", profile.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profile.getId()))
                .andExpect(jsonPath("$.nome").value("Comercial"))
                .andExpect(jsonPath("$.permissions.length()").value(2));
    }

    @Test
    void findById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/perfis/{id}", 999999L).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201WithProfile() throws Exception {
        mockMvc.perform(post("/api/perfis")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Financeiro")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Financeiro"))
                .andExpect(jsonPath("$.permissions.length()").value(2));
    }

    @Test
    void create_duplicateName_returns400WithClearMessage() throws Exception {
        createProfile("Operacao", permissions.subList(0, 1));

        mockMvc.perform(post("/api/perfis")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("operacao")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Ja existe um perfil")));
    }

    @Test
    void update_changesNameAndPermissions() throws Exception {
        Profile profile = createProfile("Operacao", permissions.subList(0, 1));
        String payload = """
                {
                  "nome": "Operacao Regional",
                  "permissionsIds": [%d, %d]
                }
                """.formatted(permissions.get(1).getId(), permissions.get(2).getId());

        mockMvc.perform(put("/api/perfis/{id}", profile.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Operacao Regional"))
                .andExpect(jsonPath("$.permissions.length()").value(2));
    }

    @Test
    void delete_removesAvailableProfile() throws Exception {
        Profile profile = createProfile("Temporario", permissions.subList(0, 1));

        mockMvc.perform(delete("/api/perfis/{id}", profile.getId()).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/perfis/{id}", profile.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_administratorProfile_returns400() throws Exception {
        Long adminProfileId = profileRepository.findByName("Administrador").orElseThrow().getId();

        mockMvc.perform(delete("/api/perfis/{id}", adminProfileId).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Administrador")));
    }

    @Test
    void delete_profileUsedByActiveCollaborator_returns400() throws Exception {
        Profile profile = createProfile("Em Uso", permissions.subList(0, 1));
        collaboratorRepository.save(new Collaborator(
                "Colaborador Ativo",
                new Date(),
                Position.OTHER,
                false,
                profile,
                null,
                null,
                null,
                null));

        mockMvc.perform(delete("/api/perfis/{id}", profile.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("colaboradores ativos")));
    }

    private Profile createProfile(String name, List<Permission> profilePermissions) {
        Profile profile = new Profile(name);
        profile.grantPermissions(profilePermissions);
        return profileRepository.save(profile);
    }

    private String validPayload(String name) {
        return """
                {
                  "nome": "%s",
                  "permissionsIds": [%d, %d]
                }
                """.formatted(name, permissions.get(0).getId(), permissions.get(1).getId());
    }
}
