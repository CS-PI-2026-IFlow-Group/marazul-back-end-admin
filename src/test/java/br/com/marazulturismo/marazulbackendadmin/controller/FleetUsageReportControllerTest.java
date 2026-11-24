package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FleetUsageReportControllerTest {

    private static final String SLUG = "uso-frota";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private CityRepository cityRepository;

    private String token;
    private City origin;
    private City destination;
    private Collaborator driver;

    @BeforeEach
    void setUp() {
        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Uso Frota", new Date(), Position.OTHER, true, adminProfile,
                "admin.uso.frota@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        driver = collaboratorRepository.save(new Collaborator(
                "Motorista Uso Frota", new Date(), Position.DRIVER,
                false, adminProfile, null, null, null, null));

        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);

        Vehicle mostUsed = saveVehicle("0001", "USO0A01", VehicleStatus.ACTIVE);
        saveVehicle("0002", "USO0A02", VehicleStatus.ACTIVE);
        Vehicle underMaintenance = saveVehicle("0003", "USO0A03", VehicleStatus.UNDER_MAINTENANCE);

        saveTrip(mostUsed, LocalDateTime.of(2031, 5, 10, 8, 0), LocalDateTime.of(2031, 5, 11, 18, 0));
        saveTrip(mostUsed, LocalDateTime.of(2031, 5, 11, 20, 0), LocalDateTime.of(2031, 5, 11, 23, 0));
        saveTrip(underMaintenance, LocalDateTime.of(2031, 4, 29, 8, 0), LocalDateTime.of(2031, 5, 3, 18, 0));
    }

    @Test
    void list_includesFleetUsageWithMonthAsDefaultPeriod() throws Exception {
        mockMvc.perform(get("/api/relatorios").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.relatorio == 'uso-frota')].titulo").value(hasItem("Uso dos ônibus")))
                .andExpect(jsonPath("$[?(@.relatorio == 'uso-frota')].periodoPadrao").value(hasItem("MES")));
    }

    @Test
    void table_returnsOneRowPerVehicleSortedByUsageWithTotals() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2031-05-01")
                        .param("dataFim", "2031-05-31")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorio").value(SLUG))
                .andExpect(jsonPath("$.titulo").value("Uso dos ônibus"))
                .andExpect(jsonPath("$.colunas[6].chave").value("aproveitamento"))
                .andExpect(jsonPath("$.colunas[6].formato").value("PERCENTUAL"))
                .andExpect(jsonPath("$.linhas[0].prefixo").value("0003"))
                .andExpect(jsonPath("$.linhas[0].situacao").value("Em manutenção"))
                .andExpect(jsonPath("$.linhas[0].viagens").value(0))
                .andExpect(jsonPath("$.linhas[0].diasRodados").value(3))
                .andExpect(jsonPath("$.linhas[0].diasParados").value(28))
                .andExpect(jsonPath("$.linhas[0].aproveitamento").value(9.7))
                .andExpect(jsonPath("$.linhas[1].prefixo").value("0001"))
                .andExpect(jsonPath("$.linhas[1].placa").value("USO0A01"))
                .andExpect(jsonPath("$.linhas[1].situacao").value("Ativo"))
                .andExpect(jsonPath("$.linhas[1].viagens").value(2))
                .andExpect(jsonPath("$.linhas[1].diasRodados").value(2))
                .andExpect(jsonPath("$.linhas[1].diasParados").value(29))
                .andExpect(jsonPath("$.linhas[1].aproveitamento").value(6.5))
                .andExpect(jsonPath("$.linhas[2].prefixo").value("0002"))
                .andExpect(jsonPath("$.linhas[2].viagens").value(0))
                .andExpect(jsonPath("$.linhas[2].diasRodados").value(0))
                .andExpect(jsonPath("$.linhas[2].aproveitamento").value(0.0))
                .andExpect(jsonPath("$.totais[0].chave").value("viagens"))
                .andExpect(jsonPath("$.totais[0].valor").value(2))
                .andExpect(jsonPath("$.totais[1].chave").value("mediaDiasRodados"))
                .andExpect(jsonPath("$.totais[2].valor").value("0001"))
                .andExpect(jsonPath("$.totais[3].valor").value("0002"));
    }

    @Test
    void table_withoutTripsInPeriod_returnsFleetZeroedAndBlankMostAndLeastUsed() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2031-08-01")
                        .param("dataFim", "2031-08-31")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[?(@.prefixo == '0001')].diasRodados").value(hasItem(0)))
                .andExpect(jsonPath("$.linhas[?(@.prefixo == '0001')].diasParados").value(hasItem(31)))
                .andExpect(jsonPath("$.totais[0].valor").value(0))
                .andExpect(jsonPath("$.totais[2].valor").doesNotExist())
                .andExpect(jsonPath("$.totais[3].valor").doesNotExist());
    }

    @Test
    void table_withoutPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("obrigatórias")));
    }

    @Test
    void table_withStartDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2031-05-31")
                        .param("dataFim", "2031-05-01")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("data inicial")));
    }

    @Test
    void pdf_returnsApplicationPdfWithFileName() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}/pdf", SLUG)
                        .param("dataInicio", "2031-05-01")
                        .param("dataFim", "2031-05-31")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("relatorio-uso-frota-2031-05-01-a-2031-05-31.pdf")));
    }

    private Vehicle saveVehicle(String prefix, String plate, VehicleStatus status) {
        return vehicleRepository.save(new Vehicle(
                prefix, plate, BodyworkModel.MARCOPOLO, VehicleType.LD, 2022, 46, null, status));
    }

    private void saveTrip(Vehicle vehicle, LocalDateTime departure, LocalDateTime arrival) {
        tripRepository.save(new Trip(origin, destination,
                departure, departure.plusMinutes(30),
                arrival.minusMinutes(30), arrival,
                vehicle, Set.of(driver)));
    }
}
