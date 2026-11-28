package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportFilterDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportSummaryDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportFilterType;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ReportFilterValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.model.Expense;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class TripResultsReportTest {

    private static final String SLUG = "resultado-viagens";
    private static final LocalDate START = LocalDate.of(2032, 3, 1);
    private static final LocalDate END = LocalDate.of(2032, 3, 31);

    @Autowired
    private TripResultsReport report;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportPdfService reportPdfService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    private City origin;
    private City destination;
    private Vehicle vehicle;
    private Client school;
    private Client church;
    private long sequence;

    @BeforeEach
    void setUp() {
        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);
        vehicle = vehicleRepository.save(new Vehicle(
                "RV01", "RVT0A01", BodyworkModel.MARCOPOLO, VehicleType.LD, 2023, 46, null, VehicleStatus.ACTIVE));
        school = saveClient("Colégio Estadual Resultado");
        church = saveClient("Paróquia Resultado");
    }

    @Test
    void isListedWithTheMonthAsDefaultPeriodAndTheClientAsOptionalFilter() {
        assertThat(reportService.available())
                .filteredOn(summary -> summary.relatorio().equals(SLUG))
                .extracting(ReportSummaryDTO::titulo, ReportSummaryDTO::exigePeriodo,
                        ReportSummaryDTO::periodoPadrao, ReportSummaryDTO::filtros)
                .containsExactly(tuple("Resultado das viagens", true, ReportDefaultPeriod.MES,
                        List.of(new ReportFilterDTO("clienteId", "Cliente", ReportFilterType.CLIENTE))));
    }

    @Test
    void columns_bringTheValuesAsMoney() {
        assertThat(report.columns())
                .extracting(ReportColumnDTO::chave, ReportColumnDTO::formato)
                .containsExactly(
                        tuple("data", ReportValueFormat.DATA),
                        tuple("trecho", ReportValueFormat.TEXTO),
                        tuple("cliente", ReportValueFormat.TEXTO),
                        tuple("situacao", ReportValueFormat.TEXTO),
                        tuple("cobrado", ReportValueFormat.MOEDA),
                        tuple("recebido", ReportValueFormat.MOEDA),
                        tuple("gastos", ReportValueFormat.MOEDA),
                        tuple("resultado", ReportValueFormat.MOEDA));
    }

    @Test
    void content_sumsPassengersAndExpensesSeparatelyForEachTrip() {
        Trip trip = saveTrip(START.atTime(6, 0), school, money("9800.00"),
                List.of(money("4000.00"), money("5100.00"), money("0.00")),
                List.of(money("2400.00"), money("850.40")));
        trip.advanceTo(TripStatus.COMPLETED, START.plusDays(1).atTime(22, 0));
        tripRepository.saveAndFlush(trip);

        List<Map<String, Object>> rows = content(Map.of()).rows();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0))
                .containsEntry("viagemId", trip.getId())
                .containsEntry("data", START)
                .containsEntry("trecho", route())
                .containsEntry("cliente", "Colégio Estadual Resultado")
                .containsEntry("situacao", "Concluída")
                .containsEntry("cobrado", money("9800.00"))
                .containsEntry("recebido", money("9100.00"))
                .containsEntry("gastos", money("3250.40"))
                .containsEntry("resultado", money("5849.60"));
    }

    @Test
    void content_allowsANegativeResult() {
        saveTrip(START.atTime(6, 0), school, money("500.00"),
                List.of(money("100.00")), List.of(money("350.00")));

        assertThat(content(Map.of()).rows().get(0)).containsEntry("resultado", money("-250.00"));
    }

    @Test
    void content_leavesTheChargedValueBlankWhenTheTripHasNoEstimate() {
        saveTrip(START.atTime(6, 0), null, null, List.of(), List.of());

        assertThat(content(Map.of()).rows().get(0))
                .containsEntry("cobrado", null)
                .containsEntry("cliente", null)
                .containsEntry("recebido", money("0.00"))
                .containsEntry("gastos", money("0.00"))
                .containsEntry("resultado", money("0.00"));
    }

    @Test
    void content_ordersByDepartureAndLeavesOutTripsOutsideThePeriod() {
        Trip later = saveTrip(START.plusDays(5).atTime(8, 0), school, null, List.of(), List.of());
        Trip earlier = saveTrip(START.atStartOfDay(), school, null, List.of(), List.of());
        saveTrip(START.minusDays(1).atTime(23, 59), school, null, List.of(), List.of());
        saveTrip(END.plusDays(1).atStartOfDay(), school, null, List.of(), List.of());

        assertThat(content(Map.of()).rows())
                .extracting(row -> row.get("viagemId"))
                .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void totals_sumTheChargedReceivedExpensesAndResultOfThePeriod() {
        saveTrip(START.atTime(6, 0), school, money("1000.00"),
                List.of(money("900.00")), List.of(money("300.00")));
        saveTrip(START.plusDays(1).atTime(6, 0), church, null,
                List.of(money("200.00")), List.of(money("450.50")));

        ReportContentDTO content = content(Map.of());

        assertThat(content.totals())
                .extracting(ReportTotalDTO::chave, ReportTotalDTO::formato, ReportTotalDTO::valor)
                .containsExactly(
                        tuple("cobrado", ReportValueFormat.MOEDA, money("1000.00")),
                        tuple("recebido", ReportValueFormat.MOEDA, money("1100.00")),
                        tuple("gastos", ReportValueFormat.MOEDA, money("750.50")),
                        tuple("resultado", ReportValueFormat.MOEDA, money("349.50")));
    }

    @Test
    void content_withClient_keepsOnlyThatClientsTripsInRowsAndTotals() {
        Trip schoolTrip = saveTrip(START.atTime(6, 0), school, money("1000.00"),
                List.of(money("900.00")), List.of(money("300.00")));
        saveTrip(START.plusDays(1).atTime(6, 0), church, money("2000.00"),
                List.of(money("2000.00")), List.of(money("100.00")));

        ReportContentDTO content = content(Map.of("clienteId", school.getId().toString()));

        assertThat(content.rows()).extracting(row -> row.get("viagemId")).containsExactly(schoolTrip.getId());
        assertThat(content.totals())
                .extracting(ReportTotalDTO::valor)
                .containsExactly(money("1000.00"), money("900.00"), money("300.00"), money("600.00"));
    }

    @Test
    void content_withUnknownClient_isRejected() {
        assertThatThrownBy(() -> content(Map.of("clienteId", "999999")))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void content_withInvalidClientId_isRejected() {
        assertThatThrownBy(() -> content(Map.of("clienteId", "abc")))
                .isInstanceOf(ReportFilterValidationException.class)
                .hasMessageContaining("clienteId");
    }

    @Test
    void pdf_showsTheValuesInBrazilianCurrency() throws IOException {
        saveTrip(START.atTime(6, 0), school, money("9800.00"),
                List.of(money("9100.00")), List.of(money("3250.40")));
        saveTrip(START.plusDays(1).atTime(6, 0), church, null,
                List.of(money("100.00")), List.of(money("350.00")));

        byte[] pdf = reportPdfService.render(reportService.generate(SLUG, START, END, Map.of()));

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("Resultado das viagens", "01/03/2032", "Colégio", "Paróquia",
                            "R$ 9.800,00", "R$ 9.100,00", "R$ 3.250,40", "R$ 5.849,60", "-R$ 250,00",
                            "Resultado: R$ 5.599,60");
        }
    }

    private ReportContentDTO content(Map<String, String> filters) {
        return report.content(ReportPeriodDTO.of(START, END), filters);
    }

    private String route() {
        return origin.getName() + "/" + origin.getState().getAcronym()
                + " – " + destination.getName() + "/" + destination.getState().getAcronym();
    }

    private Trip saveTrip(LocalDateTime departure, Client client, BigDecimal charged,
                          List<BigDecimal> paid, List<BigDecimal> expenses) {
        LocalDateTime returnDeparture = departure.plusDays(1);
        Trip trip = new Trip(origin, destination,
                departure, departure.plusHours(4), returnDeparture, returnDeparture.plusHours(4),
                vehicle, Set.of(), client, null, null);
        trip.update(origin, destination,
                departure, departure.plusHours(4), returnDeparture, returnDeparture.plusHours(4),
                vehicle, Set.of(), client, null, null, charged, null, null, null, null);

        for (BigDecimal amount : paid) {
            Passenger passenger = passengerRepository.save(new Passenger(
                    "Passageiro Resultado " + sequence, String.format("%011d", 30_000_000_000L + sequence++), null));
            trip.addPassenger(new TripPassenger(trip, passenger, amount, amount));
        }
        for (BigDecimal amount : expenses) {
            trip.addExpense(new Expense("Gasto", amount));
        }

        return tripRepository.saveAndFlush(trip);
    }

    private Client saveClient(String name) {
        Address address = addressRepository.save(new Address("Rua do Resultado", "1", null, origin));
        return clientRepository.save(new Client(
                name, String.format("%011d", 40_000_000_000L + sequence++), null, address));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
