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
}