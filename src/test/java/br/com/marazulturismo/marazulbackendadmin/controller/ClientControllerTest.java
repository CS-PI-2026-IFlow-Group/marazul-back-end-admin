package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    private String token;

    // CPF válido de teste: 529.982.247-25 (dígitos verificadores corretos)
    private static final String VALID_CPF_MASKED = "529.982.247-25";
    private static final String VALID_CPF_CLEAN = "52998224725";

    // CNPJ válido de teste: 11.222.333/0001-81 (dígitos verificadores corretos)
    private static final String VALID_CNPJ_MASKED = "11.222.333/0001-81";
    private static final String VALID_CNPJ_CLEAN = "11222333000181";

    @BeforeEach
    void setUp() {
        Collaborator admin = collaboratorRepository.findByEmail("admin@marazul.com.br")
                .orElseGet(() -> {
                    Collaborator colab = new Collaborator(
                            "Admin Test",
                            new Date(),
                            Position.OTHER,
                            true,
                            profileRepository.findByName("Administrador").orElseThrow(),
                            "admin@marazul.com.br",
                            "(11) 99999-9999",
                            null,
                            "hashed"
                    );
                    return collaboratorRepository.save(colab);
                });

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    @DisplayName("Deve cadastrar cliente com CPF mascarado com sucesso e normalizar dígitos")
    void createWithMaskedCpf_Success() throws Exception {
        String payload = """
                {
                  "name": "Maria Silva",
                  "cpf": "%s",
                  "address": {
                    "street": "Av. Brasil",
                    "number": "120",
                    "complement": "Apto 301",
                    "city": "Cabo Frio",
                    "state": "RJ"
                  }
                }
                """.formatted(VALID_CPF_MASKED);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", is("Maria Silva")))
                .andExpect(jsonPath("$.cpf", is(VALID_CPF_CLEAN)))
                .andExpect(jsonPath("$.cnpj").value(nullValue()))
                .andExpect(jsonPath("$.address.street", is("Av. Brasil")))
                .andExpect(jsonPath("$.address.city", is("Cabo Frio")))
                .andExpect(jsonPath("$.address.state", is("RJ")));
    }

    @Test
    @DisplayName("Deve cadastrar cliente com CNPJ com sucesso")
    void createWithCnpj_Success() throws Exception {
        String payload = """
                {
                  "name": "Turismo Brasil Ltda",
                  "cnpj": "%s",
                  "address": {
                    "street": "Rua Central",
                    "number": "500",
                    "city": "Armação dos Búzios",
                    "state": "RJ"
                  }
                }
                """.formatted(VALID_CNPJ_MASKED);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Turismo Brasil Ltda")))
                .andExpect(jsonPath("$.cnpj", is(VALID_CNPJ_CLEAN)))
                .andExpect(jsonPath("$.cpf").value(nullValue()));
    }

    @Test
    void createWithUnknownCity_BadRequest() throws Exception {
        String payload = """
                {
                  "name": "Maria Silva",
                  "cpf": "%s",
                  "address": {
                    "street": "Av. Brasil",
                    "number": "120",
                    "city": "Cidade Inexistente",
                    "state": "RJ"
                  }
                }
                """.formatted(VALID_CPF_CLEAN);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cidade e UF não encontradas."));
    }

    @Test
    @DisplayName("Deve rejeitar quando nem CPF nem CNPJ forem fornecidos")
    void createWithoutDocument_BadRequest() throws Exception {
        String payload = """
                {
                  "name": "Cliente Sem Documento"
                }
                """;

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("É obrigatório informar CPF ou CNPJ."));
    }

    @Test
    @DisplayName("Deve rejeitar quando ambos CPF e CNPJ forem informados")
    void createWithBothDocuments_BadRequest() throws Exception {
        String payload = """
                {
                  "name": "Cliente Ambos",
                  "cpf": "%s",
                  "cnpj": "%s"
                }
                """.formatted(VALID_CPF_CLEAN, VALID_CNPJ_CLEAN);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe apenas CPF ou CNPJ, não ambos."));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com dígitos verificadores inválidos")
    void createWithInvalidCpfDigits_BadRequest() throws Exception {
        String payload = """
                {
                  "name": "Cliente CPF Invalido",
                  "cpf": "123.456.789-00"
                }
                """;

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar quando CPF já estiver cadastrado")
    void createWithDuplicateCpf_BadRequest() throws Exception {
        Address address = addressRepository.save(new Address(
                "Rua 1", "10", null, city("Cabo Frio")));
        clientRepository.save(new Client("Existente", VALID_CPF_CLEAN, null, address));

        String payload = """
                {
                  "name": "Outro Cliente",
                  "cpf": "%s"
                }
                """.formatted(VALID_CPF_MASKED);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado: " + VALID_CPF_CLEAN));
    }

    @Test
    @DisplayName("Deve listar clientes com resumo de endereço (cidade e sigla do estado)")
    void listClients_Success() throws Exception {
        Address addr = addressRepository.save(new Address("Rua 1", "10", null, city("Arraial do Cabo")));
        clientRepository.save(new Client("João Pereira", VALID_CPF_CLEAN, null, addr));

        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("João Pereira")))
                .andExpect(jsonPath("$[0].city", is("Arraial do Cabo")))
                .andExpect(jsonPath("$[0].state", is("RJ")));
    }

    @Test
    @DisplayName("Deve filtrar clientes pelo parâmetro opcional busca por nome, CPF ou CNPJ ignorando formatação")
    void searchClients_Success() throws Exception {
        Address addr1 = addressRepository.save(new Address("Rua 1", "10", null, city("Cabo Frio")));
        Address addr2 = addressRepository.save(new Address("Rua 2", "20", null, city("Armação dos Búzios")));

        clientRepository.save(new Client("Carlos Silva", VALID_CPF_CLEAN, null, addr1));
        clientRepository.save(new Client("Empresa ABC", null, VALID_CNPJ_CLEAN, addr2));

        // Busca por parte do nome (case-insensitive)
        mockMvc.perform(get("/api/clientes?busca=carlos")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Carlos Silva")));

        // Busca por documento com pontuação/máscara
        mockMvc.perform(get("/api/clientes?busca=" + VALID_CPF_MASKED)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cpf", is(VALID_CPF_CLEAN)));
    }

    @Test
    @DisplayName("Deve obter detalhes completos do cliente por ID")
    void findById_Success() throws Exception {
        Address addr = addressRepository.save(new Address(
                "Av. Beira Mar", "99", "Bloco B", city("Arraial do Cabo")));
        Client saved = clientRepository.save(new Client("Ana Paula", VALID_CPF_CLEAN, null, addr));

        mockMvc.perform(get("/api/clientes/" + saved.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Ana Paula")))
                .andExpect(jsonPath("$.address.street", is("Av. Beira Mar")))
                .andExpect(jsonPath("$.address.number", is("99")))
                .andExpect(jsonPath("$.address.complement", is("Bloco B")))
                .andExpect(jsonPath("$.address.city", is("Arraial do Cabo")))
                .andExpect(jsonPath("$.address.state", is("RJ")));
    }

    @Test
    @DisplayName("Deve retornar 404 quando cliente não for encontrado por ID")
    void findById_NotFound() throws Exception {
        mockMvc.perform(get("/api/clientes/999999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente não encontrado: 999999"));
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao tentar acessar sem token JWT")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    private City city(String name) {
        return cityRepository.findByNameIgnoreCaseAndStateAcronymIgnoreCase(name, "RJ")
                .orElseThrow();
    }
}
