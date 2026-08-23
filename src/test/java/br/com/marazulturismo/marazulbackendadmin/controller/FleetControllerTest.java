package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FleetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String token;

    private static final String VALID_VEHICLE = """
            {
              "prefix": "1001",
              "licensePlate": "ABC1D23",
              "model": "MARCOPOLO",
              "type": "LD",
              "year": 2022,
              "seats": 46
            }
            """;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();

        User admin = userRepository.save(new User(
                "Admin Teste",
                new Date(),
                Position.DRIVER,
                UserRole.ADMIN,
                "admin@marazul.test",
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/frota"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VEHICLE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsVehicleWithAllFieldsPopulated() throws Exception {
        createValidVehicle();

        mockMvc.perform(get("/api/frota").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].prefix").value("1001"))
                .andExpect(jsonPath("$[0].licensePlate").value("ABC1D23"))
                .andExpect(jsonPath("$[0].model").value("MARCOPOLO"))
                .andExpect(jsonPath("$[0].type").value("LD"))
                .andExpect(jsonPath("$[0].year").value(2022))
                .andExpect(jsonPath("$[0].seats").value(46))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void create_withoutStatus_assignsActive() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VEHICLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.inspectionDate").doesNotExist());
    }

    @Test
    void create_incomplete_returns400WithMessagePerField() throws Exception {
        String missingRequiredFields = """
                {
                  "licensePlate": "ABC1D23"
                }
                """;

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingRequiredFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.prefix").exists())
                .andExpect(jsonPath("$.model").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.year").exists())
                .andExpect(jsonPath("$.seats").exists());
    }

    @Test
    void create_typeOutsideEnum_returns400ListingAcceptedValues() throws Exception {
        String invalidType = VALID_VEHICLE.replace("\"LD\"", "\"SEMI_LEITO\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("type")))
                .andExpect(jsonPath("$.erro").value(containsString("CONVENTIONAL")));
    }

    @Test
    void create_duplicateLicensePlate_returns409() throws Exception {
        createValidVehicle();

        String otherPrefix = VALID_VEHICLE.replace("\"1001\"", "\"1002\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherPrefix))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("ABC1D23")));
    }

    @Test
    void create_duplicateLicensePlateInLowerCase_returns409() throws Exception {
        createValidVehicle();

        String sameLicensePlateInLowerCase = VALID_VEHICLE.replace("\"ABC1D23\"", "\"abc1d23\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameLicensePlateInLowerCase))
                .andExpect(status().isConflict());
    }

    @Test
    void create_licensePlateWithHyphen_normalizesAndAccepts() throws Exception {
        String withHyphen = VALID_VEHICLE.replace("\"ABC1D23\"", "\"abc-1234\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withHyphen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("ABC1234"));
    }

    @Test
    void create_licensePlateWithHyphenDuplicatingExisting_returns409() throws Exception {
        createValidVehicle();

        String sameLicensePlateWithHyphen = VALID_VEHICLE.replace("\"ABC1D23\"", "\"abc-1d23\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameLicensePlateWithHyphen))
                .andExpect(status().isConflict());
    }

    @Test
    void listEnums_returnsFullCatalogWithLabels() throws Exception {
        mockMvc.perform(get("/api/frota/enums").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types", hasSize(3)))
                .andExpect(jsonPath("$.models", hasSize(4)))
                .andExpect(jsonPath("$.statuses", hasSize(3)))
                .andExpect(jsonPath("$.types[?(@.value=='CONVENTIONAL')].label").value("Convencional"))
                .andExpect(jsonPath("$.models[?(@.value=='IRIZAR_BRASIL')].label").value("Irizar Brasil"))
                .andExpect(jsonPath("$.statuses[?(@.value=='UNDER_MAINTENANCE')].label").value("Em manutenção"));
    }

    @Test
    void listEnums_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/frota/enums"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEnums_doesNotCollideWithFindById() throws Exception {
        mockMvc.perform(get("/api/frota/enums").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types").exists());
    }

    @Test
    void create_licensePlateInInvalidFormat_returns400() throws Exception {
        String invalidLicensePlate = VALID_VEHICLE.replace("\"ABC1D23\"", "\"12345\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLicensePlate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.licensePlate").exists());
    }

    @Test
    void delete_deactivatesAndKeepsRecordQueryable() throws Exception {
        Long id = createdVehicleId();

        mockMvc.perform(delete("/api/frota/{id}", id).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/frota/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/frota").param("status", "ACTIVE").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void update_changesStatusToUnderMaintenance() throws Exception {
        Long id = createdVehicleId();

        String underMaintenance = VALID_VEHICLE.replace(
                "\"seats\": 46", "\"seats\": 46,\n  \"status\": \"UNDER_MAINTENANCE\"");

        mockMvc.perform(put("/api/frota/{id}", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(underMaintenance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
    }

    @Test
    void findById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/frota/{id}", 9999).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void list_withStatusOutsideEnum_returns400() throws Exception {
        mockMvc.perform(get("/api/frota").param("status", "SUCATEADO").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("UNDER_MAINTENANCE")));
    }

    @Test
    void conflict_returnsSameMessageInErroAndMessage() throws Exception {
        createValidVehicle();

        String otherPrefix = VALID_VEHICLE.replace("\"1001\"", "\"1002\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherPrefix))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("ABC1D23")))
                .andExpect(jsonPath("$.message").value(containsString("ABC1D23")));
    }

    @Test
    void validationError_keepsFieldsAtRootAndAddsMessage() throws Exception {
        String missingRequiredFields = """
                {
                  "licensePlate": "ABC1D23"
                }
                """;

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingRequiredFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.prefix").exists())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unauthenticated_returnsErroAndMessage() throws Exception {
        mockMvc.perform(get("/api/frota"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void notFound_returnsErroAndMessage() throws Exception {
        mockMvc.perform(get("/api/frota/{id}", 9999).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    private void createValidVehicle() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VEHICLE))
                .andExpect(status().isCreated());
    }

    private Long createdVehicleId() throws Exception {
        createValidVehicle();
        return vehicleRepository.findAll().get(0).getId();
    }
}
