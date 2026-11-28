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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
class TripResultControllerTest {

    private static final String URL = "/api/relatorios/resultado-viagens/viagem/{viagemId}";
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2032, 5, 3, 6, 0);

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

    private String token;
    private City origin;
    private City destination;
    private Vehicle vehicle;
    private Client school;
    private long sequence;

    @BeforeEach
    void setUp() {
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Resultado Viagem", new Date(), Position.OTHER, true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.resultado.viagem@marazul.test", null, null, "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);

        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);
        Address address = addressRepository.save(new Address("Rua do Resultado", "1", null, origin));
        school = clientRepository.save(new Client("Colégio Resultado da Viagem", "60000000001", null, address));
        vehicle = vehicleRepository.save(new Vehicle(
                "RI01", "RIT0A01", BodyworkModel.MARCOPOLO, VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));
    }

    @Test
    void generate_returnsTheTripValuesWithEachExpense() throws Exception {
        Trip trip = saveTrip(school, new BigDecimal("9800.00"),
                List.of(new BigDecimal("4000.00"), new BigDecimal("5100.00")),
                List.of(new Expense("Diesel", new BigDecimal("2400.00")),
                        new Expense("Pedágio", new BigDecimal("850.40"))));

        mockMvc.perform(get(URL, trip.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viagemId").value(trip.getId()))
                .andExpect(jsonPath("$.trecho").value(route()))
                .andExpect(jsonPath("$.dataSaida").value("2032-05-03T06:00:00"))
                .andExpect(jsonPath("$.cliente").value("Colégio Resultado da Viagem"))
                .andExpect(jsonPath("$.situacao").value("Concluída"))
                .andExpect(jsonPath("$.cobrado").value(9800.00))
                .andExpect(jsonPath("$.recebido").value(9100.00))
                .andExpect(jsonPath("$.gastos").value(3250.40))
                .andExpect(jsonPath("$.resultado").value(5849.60))
                .andExpect(jsonPath("$.gastosDetalhados", hasSize(2)))
                .andExpect(jsonPath("$.gastosDetalhados[0].descricao").value("Diesel"))
                .andExpect(jsonPath("$.gastosDetalhados[0].valor").value(2400.00))
                .andExpect(jsonPath("$.gastosDetalhados[1].descricao").value("Pedágio"))
                .andExpect(jsonPath("$.gastosDetalhados[1].valor").value(850.40))
                .andExpect(jsonPath("$.emitidoEm").exists());
    }

    @Test
    void generate_withoutEstimateOrExpenses_leavesTheChargedValueBlank() throws Exception {
        Trip trip = saveTrip(null, null, List.of(), List.of());

        mockMvc.perform(get(URL, trip.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cobrado").value(nullValue()))
                .andExpect(jsonPath("$.cliente").value(nullValue()))
                .andExpect(jsonPath("$.recebido").value(0.0))
                .andExpect(jsonPath("$.gastos").value(0.0))
                .andExpect(jsonPath("$.resultado").value(0.0))
                .andExpect(jsonPath("$.gastosDetalhados", hasSize(0)));
    }

    @Test
    void generate_withUnknownTrip_returns404() throws Exception {
        mockMvc.perform(get(URL, 999999L).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void generate_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL, 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pdf_showsTheValuesAndEachExpense() throws Exception {
        Trip trip = saveTrip(school, new BigDecimal("9800.00"),
                List.of(new BigDecimal("9100.00")),
                List.of(new Expense("Diesel", new BigDecimal("2400.00")),
                        new Expense("Pedágio", new BigDecimal("850.40"))));

        byte[] pdf = mockMvc.perform(get(URL + "/pdf", trip.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("resultado-viagem-" + trip.getId() + ".pdf")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(text(pdf))
                .contains("Resultado da viagem", "03/05/2032 06:00", "Colégio Resultado da Viagem", "Concluída",
                        "Cobrado R$ 9.800,00", "Recebido R$ 9.100,00", "Gastos R$ 3.250,40",
                        "Resultado R$ 5.849,60", "Diesel R$ 2.400,00", "Pedágio R$ 850,40")
                .doesNotContain("PREJUÍZO");
    }

    @Test
    void pdf_withNegativeResult_marksTheLoss() throws Exception {
        Trip trip = saveTrip(null, null, List.of(new BigDecimal("100.00")),
                List.of(new Expense("Diesel", new BigDecimal("350.00"))));

        byte[] pdf = mockMvc.perform(get(URL + "/pdf", trip.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(text(pdf)).contains("Não informado", "Cobrado -", "PREJUÍZO", "-R$ 250,00");
    }

    @Test
    void pdf_withUnknownTrip_returns404() throws Exception {
        mockMvc.perform(get(URL + "/pdf", 999999L).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String route() {
        return origin.getName() + "/" + origin.getState().getAcronym()
                + " – " + destination.getName() + "/" + destination.getState().getAcronym();
    }

    private Trip saveTrip(Client client, BigDecimal charged, List<BigDecimal> paid, List<Expense> expenses) {
        LocalDateTime returnDeparture = DEPARTURE.plusDays(1);
        Trip trip = new Trip(origin, destination,
                DEPARTURE, DEPARTURE.plusHours(4), returnDeparture, returnDeparture.plusHours(4),
                vehicle, Set.of(), client, null, null);
        trip.update(origin, destination,
                DEPARTURE, DEPARTURE.plusHours(4), returnDeparture, returnDeparture.plusHours(4),
                vehicle, Set.of(), client, null, null, charged, null, null, null, null);
        trip.advanceTo(TripStatus.COMPLETED, returnDeparture.plusHours(4));

        for (BigDecimal amount : paid) {
            Passenger passenger = passengerRepository.save(new Passenger(
                    "Passageiro Viagem " + sequence, String.format("%011d", 70_000_000_000L + sequence++), null));
            trip.addPassenger(new TripPassenger(trip, passenger, amount, amount));
        }
        expenses.forEach(trip::addExpense);

        return tripRepository.saveAndFlush(trip);
    }
}
