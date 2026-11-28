package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PassengerListControllerTest {

    private static final String URL = "/api/relatorios/lista-passageiros/{viagemId}";
    private static final LocalDateTime DEPARTURE_ORIGIN = LocalDate.now().plusDays(30).atTime(7, 30);

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

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private TripRepository tripRepository;

    private String token;
    private String tokenWithoutPermission;
    private Trip tripWithPassengers;
    private Trip tripWithoutPassengers;

    @BeforeEach
    void setUp() {
        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Lista", new Date(), Position.OTHER, true, adminProfile,
                "admin.lista@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        Profile limited = profileRepository.save(new Profile("Perfil Sem Relatorios Lista"));
        limited.grantPermissions(permissionRepository.findAll().stream()
                .filter(permission -> permission.getBaseRoute().equals("viagens"))
                .map(Permission.class::cast)
                .toList());
        Collaborator withoutPermission = collaboratorRepository.save(new Collaborator(
                "Sem Relatorios Lista", new Date(), Position.OTHER, true, limited,
                "sem.relatorios.lista@marazul.test", null, null, "hash-irrelevante"));
        tokenWithoutPermission = "Bearer " + jwtService.generateToken(withoutPermission);

        List<City> cities = cityRepository.findAll();
        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "LP02", "LPS0A02", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 44, null, VehicleStatus.ACTIVE));
        Collaborator driver = collaboratorRepository.save(new Collaborator(
                "Motorista Lista", new Date(), Position.DRIVER, false, adminProfile,
                null, null, new CNH("12345678900", CNHType.D), null));

        Passenger responsible = passengerRepository.save(new Passenger("Beatriz Responsável", "900.000.001-01", "51999887766"));
        Passenger other = passengerRepository.save(new Passenger("Augusto Passageiro", "900.000.001-02", null));

        tripWithPassengers = new Trip(cities.get(0), cities.get(1),
                DEPARTURE_ORIGIN, DEPARTURE_ORIGIN.plusHours(4),
                DEPARTURE_ORIGIN.plusDays(2), DEPARTURE_ORIGIN.plusDays(2).plusHours(4),
                vehicle, Set.of(driver));
        tripWithPassengers.addPassenger(new TripPassenger(tripWithPassengers, responsible, new BigDecimal("100.00")));
        tripWithPassengers.addPassenger(new TripPassenger(tripWithPassengers, other, new BigDecimal("100.00")));
        tripWithPassengers.setResponsiblePassenger(responsible);
        tripRepository.saveAndFlush(tripWithPassengers);

        tripWithoutPassengers = tripRepository.saveAndFlush(new Trip(cities.get(0), cities.get(1),
                DEPARTURE_ORIGIN.plusDays(10), DEPARTURE_ORIGIN.plusDays(10).plusHours(4),
                DEPARTURE_ORIGIN.plusDays(12), DEPARTURE_ORIGIN.plusDays(12).plusHours(4),
                vehicle, Set.of(driver)));
    }

    @Test
    void json_returnsNumberedPassengersWithTheResponsibleHighlighted() throws Exception {
        mockMvc.perform(get(URL, tripWithPassengers.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripWithPassengers.getId()))
                .andExpect(jsonPath("$.vehiclePrefix").value("LP02"))
                .andExpect(jsonPath("$.drivers[0]").value("Motorista Lista"))
                .andExpect(jsonPath("$.plannedDepartureOrigin").exists())
                .andExpect(jsonPath("$.passengers", hasSize(2)))
                .andExpect(jsonPath("$.passengers[0].number").value(1))
                .andExpect(jsonPath("$.passengers[0].name").value("Augusto Passageiro"))
                .andExpect(jsonPath("$.passengers[0].responsible").value(false))
                .andExpect(jsonPath("$.passengers[1].name").value("Beatriz Responsável"))
                .andExpect(jsonPath("$.passengers[1].cpf").value("90000000101"))
                .andExpect(jsonPath("$.passengers[1].responsible").value(true))
                .andExpect(jsonPath("$.responsible.phone").value("51999887766"))
                .andExpect(jsonPath("$.totalPassengers").value(2))
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    void json_forTripWithoutPassengers_returns400WithTheReason() throws Exception {
        mockMvc.perform(get(URL, tripWithoutPassengers.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("não tem passageiros vinculados")))
                .andExpect(jsonPath("$.message", containsString("não tem passageiros vinculados")));
    }

    @Test
    void json_forUnknownTrip_returns404() throws Exception {
        mockMvc.perform(get(URL, 999999L).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void pdf_returnsPrintableDocumentWithHeaderPassengersAndSignatureLines() throws Exception {
        byte[] pdf = mockMvc.perform(get(URL + "/pdf", tripWithPassengers.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("lista-passageiros-viagem-" + tripWithPassengers.getId() + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        String text;
        try (PDDocument document = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(document);
        }
        assertThat(text)
                .contains("Marazul Turismo")
                .contains("Lista de Passageiros")
                .contains("LP02 - LPS0A02")
                .contains("Motorista Lista")
                .contains("Responsável pelo grupo".toUpperCase())
                .contains("(51) 99988-7766")
                .contains("1 Augusto Passageiro")
                .contains("900.000.001-01")
                .contains("Não informado")
                .contains("Total de passageiros: 2")
                .contains("Emitido em:")
                .contains("Conferência do motorista")
                .contains("Assinatura do responsável");
    }

    @Test
    void pdf_forTripWithoutPassengers_returns400() throws Exception {
        mockMvc.perform(get(URL + "/pdf", tripWithoutPassengers.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("não tem passageiros vinculados")));
    }

    @Test
    void pdf_forUnknownTrip_returns404() throws Exception {
        mockMvc.perform(get(URL + "/pdf", 999999L).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL, tripWithPassengers.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL + "/pdf", tripWithPassengers.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withoutReportPermission_returns403() throws Exception {
        mockMvc.perform(get(URL, tripWithPassengers.getId()).header("Authorization", tokenWithoutPermission))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/pdf", tripWithPassengers.getId()).header("Authorization", tokenWithoutPermission))
                .andExpect(status().isForbidden());
    }
}
