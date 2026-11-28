package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Expense;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripResultsReportControllerTest {

    private static final String SLUG = "resultado-viagens";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    private String token;
    private Client school;

    @BeforeEach
    void setUp() {
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Resultado", new Date(), Position.OTHER, true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.resultado@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        List<City> cities = cityRepository.findAll();
        City origin = cities.get(0);
        City destination = cities.get(1);
        Address address = addressRepository.save(new Address("Rua do Resultado", "1", null, origin));
        school = clientRepository.save(new Client("Colégio Resultado", "50000000001", null, address));
        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "RC01", "RCT0A01", BodyworkModel.MARCOPOLO, VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));

        LocalDateTime departure = LocalDateTime.of(2032, 4, 6, 6, 0);
        Trip trip = new Trip(origin, destination,
                departure, departure.plusHours(4), departure.plusDays(1), departure.plusDays(1).plusHours(4),
                vehicle, Set.of(), school, null, null);
        trip.addExpense(new Expense("Diesel", new BigDecimal("300.00")));
        tripRepository.saveAndFlush(trip);
    }

    @Test
    void list_showsTheClientFilter() throws Exception {
        mockMvc.perform(get("/api/relatorios").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].titulo", SLUG).value("Resultado das viagens"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].periodoPadrao", SLUG).value("MES"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0].chave", SLUG).value("clienteId"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0].tipo", SLUG).value("CLIENTE"));
    }

    @Test
    void table_withClient_returnsTheClientsTrips() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2032-04-01")
                        .param("dataFim", "2032-04-30")
                        .param("clienteId", school.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colunas[4].formato").value("MOEDA"))
                .andExpect(jsonPath("$.linhas", hasSize(1)))
                .andExpect(jsonPath("$.linhas[0].data").value("2032-04-06"))
                .andExpect(jsonPath("$.linhas[0].cliente").value("Colégio Resultado"))
                .andExpect(jsonPath("$.linhas[0].cobrado").value(nullValue()))
                .andExpect(jsonPath("$.linhas[0].gastos").value(300.00))
                .andExpect(jsonPath("$.linhas[0].resultado").value(-300.00))
                .andExpect(jsonPath("$.totais[3].chave").value("resultado"));
    }

    @Test
    void table_withUnknownClient_returns404() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2032-04-01")
                        .param("dataFim", "2032-04-30")
                        .param("clienteId", "999999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro", containsString("Cliente não encontrado")));
    }

    @Test
    void table_withInvalidClientId_returns400() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2032-04-01")
                        .param("dataFim", "2032-04-30")
                        .param("clienteId", "abc")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("clienteId")));
    }

    @Test
    void pdf_withClient_returnsApplicationPdf() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}/pdf", SLUG)
                        .param("dataInicio", "2032-04-01")
                        .param("dataFim", "2032-04-30")
                        .param("clienteId", school.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("relatorio-resultado-viagens-2032-04-01-a-2032-04-30.pdf")));
    }
}
