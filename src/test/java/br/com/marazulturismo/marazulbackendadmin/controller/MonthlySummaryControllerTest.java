package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Expense;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
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
import java.time.YearMonth;
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
class MonthlySummaryControllerTest {

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
    private PassengerRepository passengerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    private City origin;
    private City destination;
    private Vehicle vehicle;
    private String token;
    private long passengerSequence = 1;
    private long clientSequence = 1;
    private long vehicleSequence = 1;

    @BeforeEach
    void setUp() {
        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);
        vehicle = createVehicle();

        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Resumo Mensal", new Date(), Position.OTHER, true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.resumo.mensal@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void summary_aggregatesCompletedTripsAndOrdersTiedClientsByName() throws Exception {
        Client clientA = createClient("Cliente A");
        Client clientB = createClient("Cliente B");
        Client clientC = createClient("Cliente C");
        Client clientD = createClient("Cliente D");
        Client clientE = createClient("Cliente E");
        Client clientF = createClient("Cliente F");

        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientA,
                List.of(new BigDecimal("100.00"), new BigDecimal("50.00")),
                List.of(new BigDecimal("20.00"), new BigDecimal("10.00")));
        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientB,
                List.of(new BigDecimal("200.00")), List.of(new BigDecimal("40.00")));
        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientC,
                List.of(BigDecimal.ZERO), List.of());
        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientD,
                List.of(BigDecimal.ZERO), List.of());
        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientE,
                List.of(BigDecimal.ZERO), List.of());
        createTrip(YearMonth.of(2026, 10), TripStatus.COMPLETED, clientF,
                List.of(BigDecimal.ZERO), List.of());
        createTrip(YearMonth.of(2026, 9), TripStatus.COMPLETED, clientA,
                List.of(new BigDecimal("100.00")), List.of());
        createTrip(YearMonth.of(2026, 10), TripStatus.SCHEDULED, createClient("Cliente Z"),
                List.of(new BigDecimal("999.00")), List.of(new BigDecimal("999.00")));

        mockMvc.perform(get("/api/relatorios/resumo-mensal")
                        .param("mes", "2026-10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"))
                .andExpect(jsonPath("$.periodo.dataInicio").value("2026-10-01"))
                .andExpect(jsonPath("$.periodo.dataFim").value("2026-10-31"))
                .andExpect(jsonPath("$.indicadores", hasSize(5)))
                .andExpect(jsonPath("$.indicadores[0].chave").value("viagens"))
                .andExpect(jsonPath("$.indicadores[0].formato").value("NUMERO"))
                .andExpect(jsonPath("$.indicadores[0].valor").value(6))
                .andExpect(jsonPath("$.indicadores[0].valorAnterior").value(1))
                .andExpect(jsonPath("$.indicadores[0].variacao").value(5))
                .andExpect(jsonPath("$.indicadores[0].variacaoPercentual").value(500.0))
                .andExpect(jsonPath("$.indicadores[1].valor").value(7))
                .andExpect(jsonPath("$.indicadores[1].valorAnterior").value(1))
                .andExpect(jsonPath("$.indicadores[2].formato").value("MOEDA"))
                .andExpect(jsonPath("$.indicadores[2].valor").value(350.0))
                .andExpect(jsonPath("$.indicadores[2].valorAnterior").value(100.0))
                .andExpect(jsonPath("$.indicadores[3].valor").value(70.0))
                .andExpect(jsonPath("$.indicadores[3].variacaoPercentual").value(nullValue()))
                .andExpect(jsonPath("$.indicadores[4].valor").value(280.0))
                .andExpect(jsonPath("$.principaisClientes", hasSize(5)))
                .andExpect(jsonPath("$.principaisClientes[0].cliente").value("Cliente A"))
                .andExpect(jsonPath("$.principaisClientes[4].cliente").value("Cliente E"))
                .andExpect(jsonPath("$.emitidoEm").exists());
    }

    @Test
    void summary_forMonthWithoutCompletedTrips_returnsZerosAndEmptyRanking() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo-mensal")
                        .param("mes", "2025-01")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2025-01"))
                .andExpect(jsonPath("$.indicadores", hasSize(5)))
                .andExpect(jsonPath("$.indicadores[0].valor").value(0))
                .andExpect(jsonPath("$.indicadores[0].valorAnterior").value(0))
                .andExpect(jsonPath("$.indicadores[0].variacaoPercentual").value(nullValue()))
                .andExpect(jsonPath("$.indicadores[2].valor").value(0))
                .andExpect(jsonPath("$.principaisClientes").isEmpty());
    }

    @Test
    void summary_withoutMonth_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo-mensal").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("yyyy-MM")));
    }

    @Test
    void summary_withInvalidMonth_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo-mensal")
                        .param("mes", "2026-13")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("yyyy-MM")));
    }

    @Test
    void pdf_returnsApplicationPdfWithMonthFileName() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo-mensal/pdf")
                        .param("mes", "2026-10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("resumo-mensal-2026-10.pdf")));
    }

    private Vehicle createVehicle() {
        long sequence = vehicleSequence++;
        return vehicleRepository.save(new Vehicle(
                "MS" + sequence, "MSR" + String.format("%04d", sequence),
                BodyworkModel.MARCOPOLO, VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));
    }

    private Client createClient(String name) {
        Address address = addressRepository.save(new Address("Rua de Teste", "1", null, origin));
        String cpf = String.format("%011d", 10_000_000_000L + clientSequence++);
        return clientRepository.save(new Client(name, cpf, null, address));
    }

    private void createTrip(YearMonth month, TripStatus status, Client client,
                            List<BigDecimal> passengerAmounts, List<BigDecimal> expenseAmounts) {
        LocalDateTime departure = month.atDay(10).atTime(8, 0);
        Trip trip = new Trip(origin, destination,
                departure, departure.plusHours(3), departure.plusDays(2), departure.plusDays(2).plusHours(3),
                vehicle, Set.of(), client, null, null);
        if (status == TripStatus.COMPLETED) {
            trip.advanceTo(TripStatus.COMPLETED, departure.plusDays(2).plusHours(3));
        }

        for (BigDecimal amount : passengerAmounts) {
            Passenger passenger = passengerRepository.save(new Passenger(
                    "Passageiro " + passengerSequence,
                    String.format("%011d", 20_000_000_000L + passengerSequence++), null));
            trip.addPassenger(new TripPassenger(trip, passenger, amount, amount));
        }
        for (BigDecimal amount : expenseAmounts) {
            trip.addExpense(new Expense("Gasto teste", amount));
        }
        tripRepository.saveAndFlush(trip);
    }
}
