package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

    private static final String METRICS_URL = "/api/dashboard/metricas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${api.security.token.secret}")
    private String secret;

    private User admin;
    private String token;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(new User(
                "Admin Teste",
                new Date(),
                Position.OTHER,
                UserRole.ADMIN,
                "admin@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void metrics_withValidToken_returnsCounts() throws Exception {
        saveVehicle("1001", "ABC1D23", VehicleStatus.ACTIVE);
        saveVehicle("1002", "ABC1D24", VehicleStatus.UNDER_MAINTENANCE);
        saveEmployee("Bruno Lima", "bruno.lima@marazul.test");

        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(2))
                .andExpect(jsonPath("$.activeEmployees").value(2));
    }

    @Test
    void metrics_withEmptyDatabase_returns200WithZeros() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(0))
                .andExpect(jsonPath("$.activeEmployees").value(0));
    }

    @Test
    void metrics_countsVehiclesInEveryStatus() throws Exception {
        saveVehicle("1001", "ABC1D23", VehicleStatus.ACTIVE);
        saveVehicle("1002", "ABC1D24", VehicleStatus.INACTIVE);
        saveVehicle("1003", "ABC1D25", VehicleStatus.UNDER_MAINTENANCE);

        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(3));
    }

    @Test
    void metrics_doesNotCountDisabledEmployees() throws Exception {
        disable(saveEmployee("Bruno Lima", "bruno.lima@marazul.test"));

        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEmployees").value(1));
    }

    @Test
    void metrics_returnsOnlyTheAgreedFields() throws Exception {
        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(aMapWithSize(2)))
                .andExpect(jsonPath("$.totalVehicles").exists())
                .andExpect(jsonPath("$.activeEmployees").exists());
    }

    @Test
    void metrics_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(METRICS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void metrics_withExpiredToken_returns401() throws Exception {
        mockMvc.perform(get(METRICS_URL).header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void metrics_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get(METRICS_URL).header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private Vehicle saveVehicle(String prefix, String licensePlate, VehicleStatus status) {
        return vehicleRepository.save(new Vehicle(
                prefix,
                licensePlate,
                BodyworkModel.MARCOPOLO,
                VehicleType.LD,
                2022,
                46,
                null,
                status));
    }

    private User saveEmployee(String name, String email) {
        return userRepository.save(new User(
                name,
                new Date(),
                Position.DRIVER,
                UserRole.ADMIN,
                email,
                null,
                null,
                "hash-irrelevante"));
    }

    private void disable(User user) {
        entityManager.createQuery("UPDATE User u SET u.disabledAt = :moment WHERE u.id = :id")
                .setParameter("moment", new Date())
                .setParameter("id", user.getId())
                .executeUpdate();
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(admin.getEmail())
                .claim("id", admin.getId())
                .claim("role", admin.getUserRole().name())
                .issuedAt(new Date(now - 9L * 60 * 60 * 1000))
                .expiration(new Date(now - 60L * 1000))
                .signWith(key)
                .compact();
    }
}
