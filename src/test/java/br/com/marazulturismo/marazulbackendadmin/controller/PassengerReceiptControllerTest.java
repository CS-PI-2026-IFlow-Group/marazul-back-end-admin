package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.*;
import br.com.marazulturismo.marazulbackendadmin.model.*;
import br.com.marazulturismo.marazulbackendadmin.repository.*;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerReceiptService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PassengerReceiptControllerTest {

    private static final String URL = "/api/relatorios/recibo/{viagemId}/{passageiroId}";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CollaboratorRepository collaboratorRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private PassengerRepository passengerRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private PassengerReceiptService passengerReceiptService;
    @Autowired private EntityManager entityManager;

    private String token;
    private Trip trip;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        Profile profile = profileRepository.findByName("Administrador").orElseThrow();

        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Recibo", new Date(), Position.OTHER, true, profile,
                "admin.recibo@marazul.test", null, null, "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);

        var cities = cityRepository.findAll();

        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                "REC01", "REC0A01", BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 44, null, VehicleStatus.ACTIVE));

        LocalDateTime departure = LocalDateTime.now()
                .plusDays(10)
                .withNano(0);

        passenger = passengerRepository.save(
                new Passenger("Ana Recibo", "90000000201", null));

        trip = new Trip(
                cities.get(0),
                cities.get(1),
                departure,
                departure.plusHours(4),
                departure.plusDays(1),
                departure.plusDays(1).plusHours(4),
                vehicle,
                Set.of());

        trip.addPassenger(new TripPassenger(
                trip,
                passenger,
                new BigDecimal("200.00"),
                new BigDecimal("125.50")));

        tripRepository.saveAndFlush(trip);
    }

    @Test
    void generate_returnsReceiptDataAndKeepsNumberOnReissue() throws Exception {
        for (int emission = 0; emission < 2; emission++) {
            mockMvc.perform(get(URL, trip.getId(), passenger.getId())
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.receiptNumber")
                            .value("REC-" + trip.getId() + "-" + passenger.getId()))
                    .andExpect(jsonPath("$.passengerName").value("Ana Recibo"))
                    .andExpect(jsonPath("$.passengerCpf").value("90000000201"))
                    .andExpect(jsonPath("$.amountPaid").value(125.50))
                    .andExpect(jsonPath("$.amountPaidInWords")
                            .value("cento e vinte e cinco reais e cinquenta centavos"))
                    .andExpect(jsonPath("$.originCity")
                            .value(trip.getOriginCity().getName()))
                    .andExpect(jsonPath("$.destinationStateAcronym")
                            .value(trip.getDestinationCity().getState().getAcronym()))
                    .andExpect(jsonPath("$.plannedDepartureOrigin").exists())
                    .andExpect(jsonPath("$.companyName").value("Marazul Turismo"))
                    .andExpect(jsonPath("$.issuedAt").exists());
        }
    }

    @Test
    void generate_withoutPayment_returns400() throws Exception {
        trip.getPassengers().get(0)
                .updateAmounts(new BigDecimal("200.00"), BigDecimal.ZERO);

        mockMvc.perform(get(URL, trip.getId(), passenger.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro",
                        containsString("não possui valor pago")));
    }

    @Test
    void generate_forMissingTripOrPassenger_returns404() throws Exception {
        mockMvc.perform(get(URL, 999999L, passenger.getId())
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(URL, trip.getId(), 999999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void generate_forPassengerOutsideTrip_returns404() throws Exception {
        Passenger other = passengerRepository.save(
                new Passenger("Fora da Viagem", "90000000202", null));

        mockMvc.perform(get(URL, trip.getId(), other.getId())
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void generate_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL, trip.getId(), passenger.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generate_withoutReportPermission_returns403() throws Exception {
        Profile limited = profileRepository.save(new Profile("Sem Recibos"));

        Collaborator user = collaboratorRepository.save(new Collaborator(
                "Sem Recibos", new Date(), Position.OTHER, true, limited,
                "sem.recibos@marazul.test", null, null, "hash-irrelevante"));

        mockMvc.perform(get(URL, trip.getId(), passenger.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/pdf", trip.getId(), passenger.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/relatorios/recibo/{viagemId}/pdf", trip.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdf_returnsOnePrintablePageWithReceiptData() throws Exception {
        byte[] bytes = mockMvc.perform(
                        get(URL + "/pdf", trip.getId(), passenger.getId())
                                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        containsString("passageiro-" + passenger.getId())))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (PDDocument document = PDDocument.load(bytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            assertThat(document.getPage(0).getMediaBox().getWidth())
                    .isCloseTo(595.28f, within(0.1f));

            assertThat(document.getPage(0).getMediaBox().getHeight())
                    .isCloseTo(420.94f, within(0.1f));

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document)
                    .replaceAll("\\s+", " ");

            assertThat(text).contains(
                    "Ana Recibo",
                    "900.000.002-01",
                    "125,50",
                    "cento e vinte e cinco reais e cinquenta centavos",
                    "Marazul Turismo",
                    "REC-" + trip.getId() + "-" + passenger.getId());
        }
    }

    @Test
    void batchPdf_returnsOnePagePerPayingPassengerAndSkipsUnpaidPassengers() throws Exception {
        Passenger second = passengerRepository.save(
                new Passenger("Bruno Pago", "90000000202", null));

        Passenger unpaid = passengerRepository.save(
                new Passenger("Sem Pagamento", "90000000203", null));

        trip.addPassenger(new TripPassenger(
                trip, second, new BigDecimal("200"), new BigDecimal("200")));

        trip.addPassenger(new TripPassenger(
                trip, unpaid, new BigDecimal("200")));

        tripRepository.saveAndFlush(trip);

        byte[] bytes = mockMvc.perform(
                        get("/api/relatorios/recibo/{viagemId}/pdf", trip.getId())
                                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (PDDocument document = PDDocument.load(bytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            stripper.setStartPage(1);
            stripper.setEndPage(1);

            String firstPage = stripper.getText(document)
                    .replaceAll("\\s+", " ");

            assertThat(firstPage)
                    .contains(
                            "Ana Recibo",
                            "125,50",
                            "cento e vinte e cinco reais e cinquenta centavos")
                    .doesNotContain(
                            "Bruno Pago",
                            "Sem Pagamento",
                            "duzentos reais");

            stripper.setStartPage(2);
            stripper.setEndPage(2);

            String secondPage = stripper.getText(document)
                    .replaceAll("\\s+", " ");

            assertThat(secondPage)
                    .contains(
                            "Bruno Pago",
                            "200,00",
                            "duzentos reais")
                    .doesNotContain(
                            "Ana Recibo",
                            "Sem Pagamento",
                            "cento e vinte e cinco reais e cinquenta centavos");
        }
    }

    @Test
    void pdfs_applyValidationAndSecurity() throws Exception {
        mockMvc.perform(get(URL + "/pdf", trip.getId(), passenger.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/relatorios/recibo/{viagemId}/pdf", trip.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(URL + "/pdf", trip.getId(), 999999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/relatorios/recibo/{viagemId}/pdf", 999999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        trip.getPassengers().get(0)
                .updateAmounts(new BigDecimal("200"), BigDecimal.ZERO);

        mockMvc.perform(get(URL + "/pdf", trip.getId(), passenger.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/relatorios/recibo/{viagemId}/pdf", trip.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro",
                        containsString("não possui passageiros com valor pago")));
    }

    @Test
    void batch_fetchesPassengersAndRouteInTwoQueries() {
        for (int index = 2; index <= 6; index++) {
            Passenger additional = passengerRepository.save(new Passenger(
                    "Passageiro " + index, "9000000030" + index, null));

            trip.addPassenger(new TripPassenger(
                    trip, additional, new BigDecimal("200"), new BigDecimal("100")));
        }

        tripRepository.saveAndFlush(trip);

        Long tripId = trip.getId();

        entityManager.clear();

        var statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();

        boolean previouslyEnabled = statistics.isStatisticsEnabled();

        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {
            var receipts = passengerReceiptService.generateAll(tripId);

            assertThat(receipts).hasSize(6);

            assertThat(receipts).allSatisfy(receipt -> {
                assertThat(receipt.passengerName()).isNotBlank();
                assertThat(receipt.originStateAcronym()).isNotBlank();
                assertThat(receipt.destinationStateAcronym()).isNotBlank();
            });

            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
        }
    }
}
