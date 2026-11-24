package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripVehiclePeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetUsageReportTest {

    private static final ReportPeriodDTO NOVEMBER =
            ReportPeriodDTO.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private FleetUsageReport report;

    @Test
    void definition_exposesSlugTitleColumnsAndMonthPeriod() {
        assertThat(report.slug()).isEqualTo("uso-frota");
        assertThat(report.title()).isEqualTo("Uso dos ônibus");
        assertThat(report.requiresPeriod()).isTrue();
        assertThat(report.defaultPeriod()).isEqualTo(ReportDefaultPeriod.MES);
        assertThat(report.columns()).extracting(ReportColumnDTO::chave).containsExactly(
                "prefixo", "placa", "situacao", "viagens", "diasRodados", "diasParados", "aproveitamento");
        assertThat(report.columns()).extracting(ReportColumnDTO::formato).containsExactly(
                ReportValueFormat.TEXTO, ReportValueFormat.TEXTO, ReportValueFormat.TEXTO,
                ReportValueFormat.NUMERO, ReportValueFormat.NUMERO, ReportValueFormat.NUMERO,
                ReportValueFormat.PERCENTUAL);
    }

    @Test
    void content_withVehicleWithoutTrips_listsVehicleWithZeroCounters() {
        givenFleet(vehicle(1L, "T001", VehicleStatus.UNDER_MAINTENANCE));
        givenTrips(trip(2L, "2026-11-03T08:00", "2026-11-03T18:00"));

        Map<String, Object> row = report.content(NOVEMBER).rows().get(0);

        assertThat(row).containsExactlyEntriesOf(orderedRow(
                1L, "T001", "PLC0001", "Em manutenção", 0, 0, 30, new BigDecimal("0.0")));
    }

    @Test
    void content_withTwoTripsOnSameDay_countsTripsTwiceAndDayOnce() {
        givenFleet(vehicle(1L, "T001", VehicleStatus.ACTIVE));
        givenTrips(
                trip(1L, "2026-11-10T06:00", "2026-11-10T11:00"),
                trip(1L, "2026-11-10T14:00", "2026-11-10T22:00"));

        Map<String, Object> row = report.content(NOVEMBER).rows().get(0);

        assertThat(row)
                .containsEntry("viagens", 2)
                .containsEntry("diasRodados", 1)
                .containsEntry("diasParados", 29)
                .containsEntry("aproveitamento", new BigDecimal("3.3"));
    }

    @Test
    void content_withTripsCrossingPeriodBoundaries_clipsDaysAndCountsOnlyTripsLeavingInsidePeriod() {
        givenFleet(vehicle(1L, "T001", VehicleStatus.ACTIVE));
        givenTrips(
                trip(1L, "2026-10-29T08:00", "2026-11-02T18:00"),
                trip(1L, "2026-11-29T08:00", "2026-12-03T18:00"));

        Map<String, Object> row = report.content(NOVEMBER).rows().get(0);

        assertThat(row)
                .containsEntry("viagens", 1)
                .containsEntry("diasRodados", 4)
                .containsEntry("diasParados", 26)
                .containsEntry("aproveitamento", new BigDecimal("13.3"));
    }

    @Test
    void content_sortsByUsageDescendingAndPrefixOnTie() {
        givenFleet(
                vehicle(1L, "T003", VehicleStatus.ACTIVE),
                vehicle(2L, "T001", VehicleStatus.ACTIVE),
                vehicle(3L, "T002", VehicleStatus.ACTIVE));
        givenTrips(
                trip(1L, "2026-11-05T08:00", "2026-11-06T18:00"),
                trip(2L, "2026-11-05T08:00", "2026-11-06T18:00"),
                trip(3L, "2026-11-05T08:00", "2026-11-09T18:00"));

        List<Map<String, Object>> rows = report.content(NOVEMBER).rows();

        assertThat(rows).extracting(it -> it.get("prefixo")).containsExactly("T002", "T001", "T003");
    }

    @Test
    void content_totalsTripsAverageAndMostAndLeastUsedAmongActiveVehicles() {
        givenFleet(
                vehicle(1L, "T001", VehicleStatus.ACTIVE),
                vehicle(2L, "T002", VehicleStatus.ACTIVE),
                vehicle(3L, "T003", VehicleStatus.INACTIVE),
                vehicle(4L, "T004", VehicleStatus.UNDER_MAINTENANCE));
        givenTrips(
                trip(1L, "2026-11-02T08:00", "2026-11-04T18:00"),
                trip(1L, "2026-11-10T08:00", "2026-11-10T18:00"),
                trip(2L, "2026-11-12T08:00", "2026-11-12T18:00"),
                trip(3L, "2026-11-15T08:00", "2026-11-24T18:00"));

        List<ReportTotalDTO> totals = report.content(NOVEMBER).totals();

        assertThat(totals).containsExactly(
                new ReportTotalDTO("viagens", "Viagens no período", ReportValueFormat.NUMERO, 4),
                new ReportTotalDTO("mediaDiasRodados", "Média de dias rodados",
                        ReportValueFormat.NUMERO, new BigDecimal("3.8")),
                new ReportTotalDTO("maisUtilizado", "Ônibus mais utilizado", ReportValueFormat.TEXTO, "T001"),
                new ReportTotalDTO("menosUtilizado", "Ônibus menos utilizado", ReportValueFormat.TEXTO, "T002"));
    }

    @Test
    void content_withoutTripsInPeriod_returnsWholeFleetZeroedAndBlankMostAndLeastUsed() {
        givenFleet(
                vehicle(1L, "T001", VehicleStatus.ACTIVE),
                vehicle(2L, "T002", VehicleStatus.INACTIVE));
        givenTrips();

        ReportContentDTO content = report.content(NOVEMBER);

        assertThat(content.rows()).extracting(it -> it.get("prefixo")).containsExactly("T001", "T002");
        assertThat(content.rows()).allSatisfy(row -> assertThat(row)
                .containsEntry("viagens", 0)
                .containsEntry("diasRodados", 0)
                .containsEntry("diasParados", 30)
                .containsEntry("aproveitamento", new BigDecimal("0.0")));
        assertThat(content.totals()).extracting(ReportTotalDTO::valor)
                .containsExactly(0, new BigDecimal("0.0"), null, null);
    }

    @Test
    void content_withEmptyFleet_returnsNoRowsAndZeroAverage() {
        givenFleet();
        givenTrips();

        ReportContentDTO content = report.content(NOVEMBER);

        assertThat(content.rows()).isEmpty();
        assertThat(content.totals()).extracting(ReportTotalDTO::valor)
                .containsExactly(0, new BigDecimal("0.0"), null, null);
    }

    private void givenFleet(Vehicle... vehicles) {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicles));
    }

    private void givenTrips(TripVehiclePeriodDTO... trips) {
        when(tripRepository.findVehiclePeriodsOverlapping(NOVEMBER.start(), NOVEMBER.exclusiveEnd()))
                .thenReturn(List.of(trips));
    }

    private static Vehicle vehicle(Long id, String prefix, VehicleStatus status) {
        Vehicle vehicle = new Vehicle(prefix, "PLC" + String.format("%04d", id), BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2022, 46, null, status);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    private static TripVehiclePeriodDTO trip(Long vehicleId, String departure, String arrival) {
        return new TripVehiclePeriodDTO(vehicleId, LocalDateTime.parse(departure), LocalDateTime.parse(arrival));
    }

    private static Map<String, Object> orderedRow(Long id, String prefix, String plate, String status,
                                                  int trips, int workedDays, int idleDays, BigDecimal usage) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("veiculoId", id);
        row.put("prefixo", prefix);
        row.put("placa", plate);
        row.put("situacao", status);
        row.put("viagens", trips);
        row.put("diasRodados", workedDays);
        row.put("diasParados", idleDays);
        row.put("aproveitamento", usage);
        return row;
    }
}
