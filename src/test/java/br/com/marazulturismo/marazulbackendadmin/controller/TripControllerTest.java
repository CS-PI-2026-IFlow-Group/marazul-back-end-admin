package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
public class TripControllerTest {

    private static final LocalDateTime DEPARTURE_ORIGIN = LocalDate.now().plusDays(30).atTime(8, 0);
    private static final LocalDateTime ARRIVAL_DESTINATION = DEPARTURE_ORIGIN.plusHours(4);
    private static final LocalDateTime DEPARTURE_DESTINATION = DEPARTURE_ORIGIN.plusDays(2).plusHours(6);
    private static final LocalDateTime ARRIVAL_ORIGIN = DEPARTURE_ORIGIN.plusDays(2).plusHours(10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private Profile adminProfile;
    private String token;
    private City origin;
    private City destination;
    private Vehicle vehicle;
    private Collaborator driver;

    @BeforeEach
    void setUp() {
        adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Viagens", new Date(), Position.OTHER,
                true, adminProfile, "admin.viagens@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);

        vehicle = saveVehicle("T001", "TRP0A01", VehicleStatus.ACTIVE);
        driver = saveDriver("Motorista Um", Position.DRIVER, new CNH("12345678900", CNHType.D));
    }

    @Test
    void create_ignoresRealizedTimesSentInTheRequest() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("departureOrigin", DEPARTURE_ORIGIN.toString());
        body.put("arrivalOrigin", ARRIVAL_ORIGIN.toString());

        schedule(body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.departureOrigin").doesNotExist())
                .andExpect(jsonPath("$.arrivalOrigin").doesNotExist());
    }

    @Test
    void create_stillRejectsFieldsItDoesNotKnow() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("onibusId", vehicle.getId());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("onibusId")));
    }

    @Test
    void create_withValidRequest_returns201AndAppearsInTheListing() throws Exception {
        long id = scheduledId(validTrip());

        mockMvc.perform(get("/api/viagens/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.vehicleId").value(vehicle.getId()))
                .andExpect(jsonPath("$.minimumPassengers").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void create_withoutOriginCity_returns400NamingTheField() throws Exception {
        Map<String, Object> body = validTrip();
        body.remove("originCityId");

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originCityId").value("A cidade de origem é obrigatória."));
    }

    @Test
    void create_withUnknownCity_returns404() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("destinationCityId", 999999L);

        schedule(body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("Cidade não encontrada")));
    }

    @Test
    void create_withSameOriginAndDestination_returns400() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("destinationCityId", origin.getId());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("diferente da cidade de origem")));
    }

    @Test
    void create_withScheduleOutOfOrder_returns400NamingTheBrokenStep() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("plannedArrivalDestination", DEPARTURE_ORIGIN.minusHours(1).toString());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("chegada prevista ao destino")));
    }

    @Test
    void create_withDepartureInThePast_returns400() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("plannedDepartureOrigin", LocalDateTime.now().minusDays(1).toString());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("não pode estar no passado")));
    }

    @Test
    void create_withVehicleNotActive_returns400() throws Exception {
        Map<String, Object> body = validTrip();

        body.put("vehicleId", saveVehicle("T002", "TRP0A02", VehicleStatus.UNDER_MAINTENANCE).getId());
        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("em manutenção")));

        body.put("vehicleId", saveVehicle("T003", "TRP0A03", VehicleStatus.INACTIVE).getId());
        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("inativo")));
    }

    @Test
    void create_withUnknownVehicle_returns404() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("vehicleId", 999999L);

        schedule(body).andExpect(status().isNotFound());
    }

    @Test
    void create_withoutDrivers_returns400() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("driverIds", List.of());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.driverIds").value("Informe ao menos um motorista."));
    }

    @Test
    void create_withCollaboratorWhoIsNotDriver_returns400() throws Exception {
        Collaborator attendant = saveDriver("Atendente", Position.OTHER, new CNH("98765432100", CNHType.B));
        Map<String, Object> body = validTrip();
        body.put("driverIds", List.of(attendant.getId()));

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("não é motorista")));
    }

    @Test
    void create_withInactiveDriver_returns400() throws Exception {
        Collaborator inactive = saveDriver("Motorista Desligado", Position.DRIVER, new CNH("11122233344", CNHType.D));
        inactive.deactivate();
        Map<String, Object> body = validTrip();
        body.put("driverIds", List.of(inactive.getId()));

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("está inativo")));
    }

    @Test
    void create_withDriverWithoutCnh_returns400() throws Exception {
        Collaborator withoutCnh = saveDriver("Motorista Sem CNH", Position.DRIVER, null);
        Map<String, Object> body = validTrip();
        body.put("driverIds", List.of(withoutCnh.getId()));

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("não tem CNH cadastrada")));
    }

    @Test
    void create_withUnknownDriver_returns404NamingTheId() throws Exception {
        Map<String, Object> body = validTrip();
        body.put("driverIds", List.of(driver.getId(), 999999L));

        schedule(body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(containsString("999999")));
    }

    @Test
    void create_withVehicleAlreadyBooked_returns400NamingTheConflictingTrip() throws Exception {
        long firstTrip = scheduledId(validTrip());

        schedule(validTrip())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("viagem " + firstTrip)));
    }

    @Test
    void create_withDriverAlreadyBooked_returns400NamingTheDriver() throws Exception {
        scheduledId(validTrip());
        Map<String, Object> body = validTrip();
        body.put("vehicleId", saveVehicle("T004", "TRP0A04", VehicleStatus.ACTIVE).getId());

        schedule(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("Motorista Um já está escalado")));
    }

    @Test
    void create_startingExactlyWhenAnotherTripEnds_isAllowed() throws Exception {
        scheduledId(validTrip());
        Map<String, Object> body = validTrip();
        body.put("plannedDepartureOrigin", ARRIVAL_ORIGIN.toString());
        body.put("plannedArrivalDestination", ARRIVAL_ORIGIN.plusHours(4).toString());
        body.put("plannedDepartureDestination", ARRIVAL_ORIGIN.plusDays(1).toString());
        body.put("plannedArrivalOrigin", ARRIVAL_ORIGIN.plusDays(1).plusHours(4).toString());

        schedule(body).andExpect(status().isCreated());
    }

    private Map<String, Object> validTrip() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("originCityId", origin.getId());
        body.put("destinationCityId", destination.getId());
        body.put("plannedDepartureOrigin", DEPARTURE_ORIGIN.toString());
        body.put("plannedArrivalDestination", ARRIVAL_DESTINATION.toString());
        body.put("plannedDepartureDestination", DEPARTURE_DESTINATION.toString());
        body.put("plannedArrivalOrigin", ARRIVAL_ORIGIN.toString());
        body.put("vehicleId", vehicle.getId());
        body.put("driverIds", List.of(driver.getId()));
        return body;
    }

    private ResultActions schedule(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/viagens")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private Vehicle saveVehicle(String prefix, String licensePlate, VehicleStatus status) {
        return vehicleRepository.save(new Vehicle(
                prefix, licensePlate, BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 44, null, status));
    }

    private Collaborator saveDriver(String name, Position position, CNH cnh) {
        return collaboratorRepository.save(new Collaborator(
                name, new Date(), position, false, adminProfile, null, null, cnh, null));
    }

    private long scheduledId(Map<String, Object> body) throws Exception {
        String json = schedule(body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}