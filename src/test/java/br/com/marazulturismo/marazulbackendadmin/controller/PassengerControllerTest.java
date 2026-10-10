package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
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
class PassengerControllerTest {

    private static final String VALID_PASSENGER = """
            {
              "name": "Maria Souza",
              "cpf": "529.982.247-25",
              "phone": "(41) 99999-8888"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private EntityManager entityManager;

    private String token;

    @BeforeEach
    void setUp() {
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Administrador Passageiro",
                new Date(),
                Position.DRIVER,
                true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.passageiro@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void create_returns201WithNormalizedCpfAndPhone() throws Exception {
        mockMvc.perform(post("/api/passageiros")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.phone").value("41999998888"));
    }

    @Test
    void create_withoutPhone_returns201() throws Exception {
        String payload = """
                {
                  "name": "Passageiro Sem Telefone",
                  "cpf": "52998224725"
                }
                """;

        mockMvc.perform(post("/api/passageiros")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void create_withoutRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/passageiros")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.cpf").value("O CPF é obrigatório."));
    }

    @Test
    void create_withCpfAlreadyRegistered_returns400() throws Exception {
        persistPassenger("Passageiro Existente", "52998224725", null);

        mockMvc.perform(post("/api/passageiros")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("CPF já cadastrado")));
    }

    @Test
    void list_returnsPassengersOrderedByName() throws Exception {
        persistPassenger("Zuleica Lima", "11144477735", null);
        persistPassenger("Ana Paula", "52998224725", null);

        mockMvc.perform(get("/api/passageiros").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Ana Paula"))
                .andExpect(jsonPath("$[1].name").value("Zuleica Lima"));
    }

    @Test
    void list_withBuscaByNameFragment_ignoresCase() throws Exception {
        persistPassenger("Ana Paula", "52998224725", null);
        persistPassenger("Zuleica Lima", "11144477735", null);

        mockMvc.perform(get("/api/passageiros").param("busca", "ana pau")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ana Paula"));
    }

    @Test
    void list_withBuscaByDocumentFragment_ignoresPunctuation() throws Exception {
        persistPassenger("Ana Paula", "52998224725", null);
        persistPassenger("Zuleica Lima", "11144477735", null);

        mockMvc.perform(get("/api/passageiros").param("busca", "529.982")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cpf").value("52998224725"));
    }

    @Test
    void findById_returnsPassenger() throws Exception {
        Passenger passenger = persistPassenger("Maria Souza", "52998224725", "41999998888");

        mockMvc.perform(get("/api/passageiros/{id}", passenger.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passenger.getId()))
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.phone").value("41999998888"));
    }

    @Test
    void findById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/passageiros/{id}", 999999)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Passageiro não encontrado")));
    }

    @Test
    void update_returns200WithUpdatedData() throws Exception {
        Passenger passenger = persistPassenger("Nome Antigo", "11144477735", "4133334444");

        mockMvc.perform(put("/api/passageiros/{id}", passenger.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passenger.getId()))
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.phone").value("41999998888"));
    }

    @Test
    void update_withCpfOfAnotherPassenger_returns400() throws Exception {
        Passenger passenger = persistPassenger("Nome Antigo", "11144477735", null);
        persistPassenger("Outro Passageiro", "52998224725", null);

        mockMvc.perform(put("/api/passageiros/{id}", passenger.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("CPF já cadastrado")));
    }

    @Test
    void update_withUnknownId_returns404() throws Exception {
        mockMvc.perform(put("/api/passageiros/{id}", 999999)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Passageiro não encontrado")));
    }

    @Test
    void delete_removesPassenger() throws Exception {
        Passenger passenger = persistPassenger("Passageiro Para Exclusão", "52998224725", null);
        Long passengerId = passenger.getId();

        mockMvc.perform(delete("/api/passageiros/{id}", passengerId)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(passengerRepository.existsById(passengerId)).isFalse();
    }

    @Test
    void delete_withUnknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/passageiros/{id}", 999999)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Passageiro não encontrado")));
    }

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/passageiros"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_withoutAuthentication_returns401() throws Exception {
        Passenger passenger = persistPassenger("Passageiro Protegido", "52998224725", null);

        mockMvc.perform(delete("/api/passageiros/{id}", passenger.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Passenger persistPassenger(String name, String cpf, String phone) {
        return passengerRepository.saveAndFlush(new Passenger(name, cpf, phone));
    }
}
