package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.StateRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LocationControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CollaboratorRepository collaboratorRepository;
    @Autowired StateRepository stateRepository;

    private String token;
    private Long spId;

    @BeforeEach
    void setUp() {
        token = "Bearer " + jwtService.generateToken(
                collaboratorRepository.findByEmail("admin@marazul.com.br").orElseThrow());
        spId = stateRepository.findByAcronym("SP").orElseThrow().getId();
    }

    @Test
    void states_areOrderedByNameAndExposeExpectedFields() throws Exception {
        mockMvc.perform(get("/api/estados").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").value("Acre"))
                .andExpect(jsonPath("$[0].acronym").value("AC"));
    }

    @Test
    void cities_requireStateAndFilterByNameIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/cidades").header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/cidades").param("estadoId", spId.toString())
                        .param("name", "cAmPiNaS").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Campinas"))
                .andExpect(jsonPath("$[0].stateId").value(spId))
                .andExpect(jsonPath("$[0].stateSigla").value("SP"));
    }

    @Test
    void cities_acceptStateIdAliasAndReturnOrderedResults() throws Exception {
        mockMvc.perform(get("/api/cidades").param("stateId", spId.toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(1)))
                .andExpect(jsonPath("$[0].name").value("Adamantina"));
    }

    @Test
    void cityDetail_exposesLinkedStateAndUnknownCityReturns404() throws Exception {
        String body = mockMvc.perform(get("/api/cidades").param("estadoId", spId.toString())
                        .param("name", "Campinas").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        Long cityId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get(0).get("id").asLong();

        mockMvc.perform(get("/api/cidades/{id}", cityId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Campinas"))
                .andExpect(jsonPath("$.stateId").value(spId))
                .andExpect(jsonPath("$.stateNome").value("São Paulo"))
                .andExpect(jsonPath("$.stateSigla").value("SP"));

        mockMvc.perform(get("/api/cidades/{id}", Long.MAX_VALUE).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/estados")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cidades").param("estadoId", spId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
