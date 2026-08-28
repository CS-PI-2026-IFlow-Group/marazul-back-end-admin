package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerDatabaseFailureTest {

    private static final String METRICS_URL = "/api/dashboard/metricas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    @Value("${api.security.token.secret}")
    private String secret;

    private String token;

    @BeforeEach
    void setUp() {
        token = "Bearer " + validToken();

        when(vehicleRepository.count())
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
    }

    @Test
    void metrics_whenDatabaseFails_returns500() throws Exception {
        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void metrics_whenDatabaseFails_doesNotLeakInfrastructureDetails() throws Exception {
        mockMvc.perform(get(METRICS_URL).header("Authorization", token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.erro").value(not(containsString("Exception"))))
                .andExpect(jsonPath("$.erro").value(not(containsString("connection refused"))));
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject("admin@marazul.test")
                .claim("id", 1L)
                .claim("role", UserRole.ADMIN.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60L * 60 * 1000))
                .signWith(key)
                .compact();
    }
}
