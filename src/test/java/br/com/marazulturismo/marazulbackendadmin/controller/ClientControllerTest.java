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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private EntityManager entityManager;

    private City city;
    private String token;

    @BeforeEach
    void setUp() {
        city = cityRepository.findAll().get(0);

        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Administrador Cliente",
                new Date(),
                Position.DRIVER,
                true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.cliente@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void createAndReadClient_withCityId() throws Exception {
        String payload = """
                {
                  "name": "Cliente Consulta",
                  "cpf": "529.982.247-25",
                  "address": {
                    "street": "Rua das Flores",
                    "number": "42",
                    "cityId": %d
                  }
                }
                """.formatted(city.getId());

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.address.cityId").value(city.getId()))
                .andExpect(jsonPath("$.address.city").value(city.getName()))
                .andExpect(jsonPath("$.address.state").value(city.getState().getAcronym()));

        mockMvc.perform(get("/api/clientes").param("busca", "529.982")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cliente Consulta"));

        Long id = clientRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/clientes/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.city").value(city.getName()));
    }

    @Test
    void update_reusesAddressAndSwitchesCpfToCnpj() throws Exception {
        Client client = persistClient("Cliente Original", "111.444.777-35", null, "100");
        Long addressId = client.getAddress().getId();

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCnpjPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getId()))
                .andExpect(jsonPath("$.name").value("Empresa Atualizada"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.cnpj").value("12345678000195"))
                .andExpect(jsonPath("$.address.id").value(addressId))
                .andExpect(jsonPath("$.address.street").value("Avenida Atualizada"))
                .andExpect(jsonPath("$.address.number").value("200"))
                .andExpect(jsonPath("$.address.cityId").value(city.getId()));
    }

    @Test
    void update_withInvalidCheckDigits_returns400() throws Exception {
        Client client = persistClient("Cliente Original", "11144477735", null, "101");
        String payload = validCnpjPayload()
                .replace("\"cnpj\": \"12.345.678/0001-95\"", "\"cpf\": \"123.456.789-00\", \"cnpj\": null");

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CPF inválido."));
    }

    @Test
    void update_withDocumentAlreadyAssignedToAnotherClient_returns400() throws Exception {
        Client client = persistClient("Cliente Original", "11144477735", null, "102");
        persistClient("Outro Cliente", "52998224725", null, "103");
        String payload = validCnpjPayload()
                .replace("\"cnpj\": \"12.345.678/0001-95\"", "\"cpf\": \"529.982.247-25\", \"cnpj\": null");

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("CPF já cadastrado")));
    }

    @Test
    void update_withMissingAddressField_returns400() throws Exception {
        Client client = persistClient("Cliente Original", "11144477735", null, "104");
        String payload = validCnpjPayload().replace("\"street\": \"Avenida Atualizada\",", "");

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['address.street']").value("A rua é obrigatória."));
    }

    @Test
    void update_withUnknownCity_returns400() throws Exception {
        Client client = persistClient("Cliente Original", "11144477735", null, "105");
        String payload = validCnpjPayload().replace("\"cityId\": " + city.getId(), "\"cityId\": 999999");

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Cidade não encontrada."));
    }

    @Test
    void update_withUnknownClient_returns404() throws Exception {
        mockMvc.perform(put("/api/clientes/{id}", 999999)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCnpjPayload()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Cliente não encontrado")));
    }

    @Test
    void update_withoutAuthentication_returns401() throws Exception {
        Client client = persistClient("Cliente Original", "11144477735", null, "106");

        mockMvc.perform(put("/api/clientes/{id}", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCnpjPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_removesClientAndLinkedAddress() throws Exception {
        Client client = persistClient("Cliente para Exclusão", "11144477735", null, "107");
        Long clientId = client.getId();
        Long addressId = client.getAddress().getId();

        mockMvc.perform(delete("/api/clientes/{id}", clientId)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        entityManager.clear();

        org.assertj.core.api.Assertions.assertThat(clientRepository.existsById(clientId)).isFalse();
        org.assertj.core.api.Assertions.assertThat(addressRepository.existsById(addressId)).isFalse();
    }

    @Test
    void delete_withUnknownClient_returns404() throws Exception {
        mockMvc.perform(delete("/api/clientes/{id}", 999999)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Cliente não encontrado")));
    }

    @Test
    void delete_withoutAuthentication_returns401() throws Exception {
        Client client = persistClient("Cliente para Exclusão", "11144477735", null, "108");

        mockMvc.perform(delete("/api/clientes/{id}", client.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Client persistClient(String name, String cpf, String cnpj, String number) {
        Address address = new Address("Rua Original", number, null, city);
        entityManager.persist(address);

        return clientRepository.saveAndFlush(new Client(name, cpf, cnpj, address));
    }

    private String validCnpjPayload() {
        return """
                {
                  "name": "Empresa Atualizada",
                  "cnpj": "12.345.678/0001-95",
                  "address": {
                    "street": "Avenida Atualizada",
                    "number": "200",
                    "complement": "Sala 10",
                    "cityId": %d
                  }
                }
                """.formatted(city.getId());
    }
}
