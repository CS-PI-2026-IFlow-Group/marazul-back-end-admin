package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServiceOrderControllerTest {

    private static final String URL = "/api/relatorios/ordem-servico/{viagemId}";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CollaboratorRepository collaboratorRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private PassengerRepository passengerRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TripRepository tripRepository;

    private String token;
    private String tokenWithoutPermission;
    private Trip scheduledTrip;
    private Trip tripWithoutResponsible;
    private Trip tripWithLongNotes;

    @BeforeEach
    void setUp() {
        Profile adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Ordem", new Date(), Position.OTHER, true, adminProfile,
                "admin.ordem@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        Profile limited = profileRepository.save(new Profile("Perfil Sem Permissao Ordem"));
        limited.grantPermissions(permissionRepository.findAll().stream()
                .filter(permission -> permission.getBaseRoute().equals("viagens"))
                .toList());
        Collaborator withoutPermission = collaboratorRepository.save(new Collaborator(
                "Sem Permissao Ordem", new Date(), Position.OTHER, true, limited,
                "sem.permissao.ordem@marazul.test", null, null, "hash-irrelevante"));
        tokenWithoutPermission = "Bearer " + jwtService.generateToken(withoutPermission);

        List<City> cities = cityRepository.findAll();
        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "OS01", "OSX0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2024, 44, null, VehicleStatus.ACTIVE));
        Collaborator driver = collaboratorRepository.save(new Collaborator(
                "Motorista Ordem", new Date(), Position.DRIVER, false, adminProfile,
                null, null, null, null));
        Passenger responsible = passengerRepository.save(new Passenger(
                "Contato da Viagem", "90000000201", "51999887766"));
        Address clientAddress = addressRepository.save(new Address("Rua Um", "10", null, cities.get(0)));
        Client client = clientRepository.save(new Client("Cliente Contratante", "12345678901", null, clientAddress));

        LocalDateTime departure = LocalDate.now().plusDays(40).atTime(7, 30);
        scheduledTrip = new Trip(cities.get(0), cities.get(1), departure, departure.plusHours(4),
                departure.plusDays(2), departure.plusDays(2).plusHours(4), vehicle, Set.of(driver),
                client, null, "Parada para embarque no terminal.");
        scheduledTrip.setResponsiblePassenger(responsible);
        tripRepository.saveAndFlush(scheduledTrip);

        tripWithoutResponsible = tripRepository.saveAndFlush(new Trip(cities.get(0), cities.get(1),
                departure.plusDays(10), departure.plusDays(10).plusHours(4),
                departure.plusDays(12), departure.plusDays(12).plusHours(4), vehicle, Set.of(driver)));
        tripWithLongNotes = tripRepository.saveAndFlush(new Trip(cities.get(0), cities.get(1),
                departure.plusDays(20), departure.plusDays(20).plusHours(4),
                departure.plusDays(22), departure.plusDays(22).plusHours(4), vehicle, Set.of(driver),
                null, null, "Instrução operacional de embarque. ".repeat(350)));
    }

    @Test
    void json_containsTripRouteScheduleVehicleDriversClientAndContact() throws Exception {
        mockMvc.perform(get(URL, scheduledTrip.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(scheduledTrip.getId()))
                .andExpect(jsonPath("$.originCity").exists())
                .andExpect(jsonPath("$.originState").exists())
                .andExpect(jsonPath("$.destinationCity").exists())
                .andExpect(jsonPath("$.destinationState").exists())
                .andExpect(jsonPath("$.plannedDepartureOrigin").exists())
                .andExpect(jsonPath("$.plannedArrivalDestination").exists())
                .andExpect(jsonPath("$.plannedDepartureDestination").exists())
                .andExpect(jsonPath("$.plannedArrivalOrigin").exists())
                .andExpect(jsonPath("$.vehiclePrefix").value("OS01"))
                .andExpect(jsonPath("$.vehicleLicensePlate").value("OSX0A01"))
                .andExpect(jsonPath("$.drivers[0]").value("Motorista Ordem"))
                .andExpect(jsonPath("$.clientName").value("Cliente Contratante"))
                .andExpect(jsonPath("$.responsiblePassengerName").value("Contato da Viagem"))
                .andExpect(jsonPath("$.responsiblePassengerPhone").value("51999887766"))
                .andExpect(jsonPath("$.notes").value("Parada para embarque no terminal."));
    }

    @Test
    void json_withoutResponsibleReturnsBlankContactFields() throws Exception {
        mockMvc.perform(get(URL, tripWithoutResponsible.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsiblePassengerName").value(nullValue()))
                .andExpect(jsonPath("$.responsiblePassengerPhone").value(nullValue()));
    }

    @Test
    void missingTripReturns404() throws Exception {
        mockMvc.perform(get(URL, 999999L).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void pdfIsSinglePageAndIncludesOrderDetailsEvenWithoutResponsible() throws Exception {
        byte[] pdf = mockMvc.perform(get(URL + "/pdf", tripWithoutResponsible.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("ordem-servico-viagem-" + tripWithoutResponsible.getId() + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("Marazul Turismo")
                    .contains("Ordem de Servi")
                    .contains("OS01 - OSX0A01")
                    .contains("Motorista Ordem")
                    .contains("PREVISTOS")
                    .contains("CONTATO DO PASSAGEIRO")
                    .contains("OBSERVA");
        }
    }

    @Test
    void pdfIncludesClientResponsibleAndNotesWhenAvailable() throws Exception {
        byte[] pdf = mockMvc.perform(get(URL + "/pdf", scheduledTrip.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            assertThat(text)
                    .contains("Cliente Contratante")
                    .contains("Contato da Viagem")
                    .contains("(51) 99988-7766")
                    .contains("Parada para embarque no terminal.")
                    .contains(formatter.format(scheduledTrip.getPlannedDepartureOrigin()))
                    .contains(formatter.format(scheduledTrip.getPlannedArrivalDestination()))
                    .contains(formatter.format(scheduledTrip.getPlannedDepartureDestination()))
                    .contains(formatter.format(scheduledTrip.getPlannedArrivalOrigin()));
        }
    }

    @Test
    void pdfKeepsLongNotesOnOnePage() throws Exception {
        byte[] pdf = mockMvc.perform(get(URL + "/pdf", tripWithLongNotes.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Instrução operacional de embarque.");
        }
    }

    @Test
    void reportRouteRequiresAuthenticationAndReportPermission() throws Exception {
        mockMvc.perform(get(URL, scheduledTrip.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL, scheduledTrip.getId()).header("Authorization", tokenWithoutPermission))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/pdf", scheduledTrip.getId()).header("Authorization", tokenWithoutPermission))
                .andExpect(status().isForbidden());
    }
}
