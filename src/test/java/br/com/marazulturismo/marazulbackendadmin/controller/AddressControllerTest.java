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
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CityRepository cityRepository;

    private City city;
    private String token;

    @BeforeEach
    void setUp() {
        city = cityRepository.findAll().get(0);

        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Administrador Endereços",
                new Date(),
                Position.OTHER,
                true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.enderecos@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void list_returnsAddressWithCityStateAndClient() throws Exception {
        Client client = persistClientWithAddress();

        mockMvc.perform(get("/api/enderecos").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(client.getAddress().getId()))
                .andExpect(jsonPath("$[0].street").value("Rua das Flores"))
                .andExpect(jsonPath("$[0].number").value("100"))
                .andExpect(jsonPath("$[0].complement").value("Casa"))
                .andExpect(jsonPath("$[0].cityId").value(city.getId()))
                .andExpect(jsonPath("$[0].cityName").value(city.getName()))
                .andExpect(jsonPath("$[0].stateId").value(city.getState().getId()))
                .andExpect(jsonPath("$[0].stateName").value(city.getState().getName()))
                .andExpect(jsonPath("$[0].stateAcronym").value(city.getState().getAcronym()))
                .andExpect(jsonPath("$[0].clientId").value(client.getId()))
                .andExpect(jsonPath("$[0].clientName").value("Cliente de Endereço"));
    }

    @Test
    void findById_returnsCompleteAddress() throws Exception {
        Client client = persistClientWithAddress();

        mockMvc.perform(get("/api/enderecos/{id}", client.getAddress().getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getAddress().getId()))
                .andExpect(jsonPath("$.cityName").value(city.getName()))
                .andExpect(jsonPath("$.stateAcronym").value(city.getState().getAcronym()))
                .andExpect(jsonPath("$.clientName").value("Cliente de Endereço"));
    }

    @Test
    void findById_withUnknownAddress_returns404() throws Exception {
        mockMvc.perform(get("/api/enderecos/{id}", 999999)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Endereço não encontrado")));
    }

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/enderecos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_isNotAvailable() throws Exception {
        mockMvc.perform(post("/api/enderecos").header("Authorization", token))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void update_changesAddressFields() throws Exception {
        Client client = persistClientWithAddress();
        Long addressId = client.getAddress().getId();

        mockMvc.perform(put("/api/enderecos/{id}", addressId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddressPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId))
                .andExpect(jsonPath("$.street").value("Avenida Atualizada"))
                .andExpect(jsonPath("$.number").value("200"))
                .andExpect(jsonPath("$.complement").value("Sala 10"))
                .andExpect(jsonPath("$.cityId").value(city.getId()));
    }

    @Test
    void update_withMissingStreet_returns400() throws Exception {
        Client client = persistClientWithAddress();
        String payload = validAddressPayload().replace("\"street\": \"Avenida Atualizada\",", "");

        mockMvc.perform(put("/api/enderecos/{id}", client.getAddress().getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.street").value("A rua é obrigatória."));
    }

    @Test
    void update_withUnknownCity_returns400() throws Exception {
        Client client = persistClientWithAddress();
        String payload = validAddressPayload().replace("\"cityId\": " + city.getId(), "\"cityId\": 999999");

        mockMvc.perform(put("/api/enderecos/{id}", client.getAddress().getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Cidade não encontrada."));
    }

    @Test
    void delete_withLinkedClient_returns400AndKeepsAddress() throws Exception {
        Client client = persistClientWithAddress();
        Long addressId = client.getAddress().getId();

        mockMvc.perform(delete("/api/enderecos/{id}", addressId)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("vinculado a um cliente")));

        org.assertj.core.api.Assertions.assertThat(addressRepository.existsById(addressId)).isTrue();
    }

    @Test
    void delete_withoutLinkedClient_removesAddress() throws Exception {
        Address address = addressRepository.saveAndFlush(
                new Address("Rua Sem Cliente", "101", null, city));

        mockMvc.perform(delete("/api/enderecos/{id}", address.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(addressRepository.existsById(address.getId())).isFalse();
    }

    private Client persistClientWithAddress() {
        Address address = addressRepository.saveAndFlush(
                new Address("Rua das Flores", "100", "Casa", city));

        return clientRepository.saveAndFlush(
                new Client("Cliente de Endereço", "111.444.777-35", null, address));
    }

    private String validAddressPayload() {
        return """
                {
                  "street": "Avenida Atualizada",
                  "number": "200",
                  "complement": "Sala 10",
                  "cityId": %d
                }
                """.formatted(city.getId());
    }
}
