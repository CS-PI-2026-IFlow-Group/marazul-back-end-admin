package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportSummaryDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTableResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import br.com.marazulturismo.marazulbackendadmin.exception.ReportPeriodValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@SpringBootTest
@Transactional
class WeeklyScheduleReportTest {

    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);

    @Autowired
    private WeeklyScheduleReport report;

    @Autowired
    private ReportService reportService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private City origin;
    private City destination;
    private Vehicle busOne;
    private Vehicle busTwo;
    private Collaborator zeca;
    private Collaborator alberto;

    @BeforeEach
    void setUp() {
        List<City> cities = cityRepository.findAll();
        origin = cities.get(0);
        destination = cities.get(1);
        busOne = saveVehicle("AG01", "AGD0A01", VehicleStatus.ACTIVE);
        busTwo = saveVehicle("AG02", "AGD0A02", VehicleStatus.ACTIVE);
        zeca = saveDriver("Zeca Motorista", "11122233300");
        alberto = saveDriver("Alberto Motorista", "44455566600");
    }

    @Test
    void isListedAmongTheAvailableReportsWithAPeriod() {
        assertThat(reportService.available())
                .filteredOn(summary -> summary.relatorio().equals("agenda"))
                .extracting(ReportSummaryDTO::titulo, ReportSummaryDTO::exigePeriodo)
                .containsExactly(tuple("Agenda da semana", true));
    }

    @Test
    void generate_returnsTheScheduleColumnsInOrder() {
        ReportTableResponseDTO table = reportService.generate("agenda", MONDAY, MONDAY.plusDays(6));

        assertThat(table.colunas())
                .extracting(ReportColumnDTO::chave)
                .containsExactly("dia", "diaSemana", "saida", "trecho", "onibus", "motoristas", "situacao");
    }

    @Test
    void generate_withoutPeriod_isRejected() {
        assertThatThrownBy(() -> reportService.generate("agenda", null, null))
                .isInstanceOf(ReportPeriodValidationException.class);
    }

    @Test
    void content_listsTheTripsOfTheDayOrderedByDeparture() {
        Trip afternoon = saveTrip(busOne, MONDAY.atTime(14, 0), Set.of(zeca, alberto));
        Trip morning = saveTrip(busTwo, MONDAY.atTime(6, 30), Set.of(zeca));

        List<Map<String, Object>> rows = content(MONDAY, MONDAY).rows();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .containsEntry("viagemId", morning.getId())
                .containsEntry("dia", MONDAY)
                .containsEntry("diaSemana", "Segunda-feira")
                .containsEntry("saida", "06:30")
                .containsEntry("trecho", route())
                .containsEntry("onibus", "AG02")
                .containsEntry("motoristas", "Zeca Motorista")
                .containsEntry("situacao", "Agendada");
        assertThat(rows.get(1))
                .containsEntry("viagemId", afternoon.getId())
                .containsEntry("saida", "14:00")
                .containsEntry("onibus", "AG01")
                .containsEntry("motoristas", "Alberto Motorista, Zeca Motorista");
    }

    @Test
    void content_showsEachDayWithoutTripInsteadOfOmittingIt() {
        Trip tuesday = saveTrip(busOne, MONDAY.plusDays(1).atTime(8, 0), Set.of(zeca));

        List<Map<String, Object>> rows = content(MONDAY, MONDAY.plusDays(2)).rows();

        assertThat(rows)
                .extracting(row -> row.get("dia"), row -> row.get("diaSemana"), row -> row.get("situacao"))
                .containsExactly(
                        tuple(MONDAY, "Segunda-feira", "Sem viagem"),
                        tuple(MONDAY.plusDays(1), "Terça-feira", "Agendada"),
                        tuple(MONDAY.plusDays(2), "Quarta-feira", "Sem viagem"));
        assertThat(rows.get(0))
                .containsEntry("viagemId", null)
                .containsEntry("saida", null)
                .containsEntry("trecho", null)
                .containsEntry("onibus", null)
                .containsEntry("motoristas", null);
        assertThat(rows.get(1)).containsEntry("viagemId", tuesday.getId());
    }

    @Test
    void content_leavesOutTripsDepartingOutsideThePeriod() {
        saveTrip(busOne, MONDAY.minusDays(1).atTime(23, 59), Set.of(zeca));
        saveTrip(busTwo, MONDAY.plusDays(1).atStartOfDay(), Set.of(zeca));

        List<Map<String, Object>> rows = content(MONDAY, MONDAY).rows();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("situacao", "Sem viagem");
    }

    @Test
    void totals_countTheTripsAndTheDistinctBuses() {
        saveTrip(busOne, MONDAY.atTime(7, 0), Set.of(zeca));
        saveTrip(busOne, MONDAY.plusDays(1).atTime(7, 0), Set.of(zeca));
        saveTrip(busTwo, MONDAY.plusDays(2).atTime(7, 0), Set.of(alberto));

        ReportContentDTO content = content(MONDAY, MONDAY.plusDays(2));

        assertThat(total(content, "viagens")).isEqualTo(3L);
        assertThat(total(content, "onibusEnvolvidos")).isEqualTo(2L);
    }

    @Test
    void totals_listOnlyActiveBusesWithoutTripInThePeriod() {
        saveTrip(busOne, MONDAY.atTime(7, 0), Set.of(zeca));
        Vehicle returning = saveVehicle("AG03", "AGD0A03", VehicleStatus.ACTIVE);
        saveTrip(returning, MONDAY.minusDays(2).atTime(8, 0), MONDAY.atTime(10, 0), Set.of(alberto));
        saveVehicle("AG04", "AGD0A04", VehicleStatus.INACTIVE);
        saveVehicle("AG05", "AGD0A05", VehicleStatus.UNDER_MAINTENANCE);

        Object idle = total(content(MONDAY, MONDAY.plusDays(6)), "onibusSemViagem");

        assertThat(idle).asInstanceOf(list(String.class))
                .contains("AG02")
                .doesNotContain("AG01", "AG03", "AG04", "AG05");
    }

    private ReportContentDTO content(LocalDate start, LocalDate end) {
        return report.content(ReportPeriodDTO.of(start, end));
    }

    private static Object total(ReportContentDTO content, String key) {
        return content.totals().stream()
                .filter(total -> total.chave().equals(key))
                .map(ReportTotalDTO::valor)
                .findFirst()
                .orElseThrow();
    }

    private String route() {
        return origin.getName() + "/" + origin.getState().getAcronym()
                + " → " + destination.getName() + "/" + destination.getState().getAcronym();
    }

    private Trip saveTrip(Vehicle vehicle, LocalDateTime departure, Set<Collaborator> drivers) {
        return saveTrip(vehicle, departure, departure.plusHours(12), drivers);
    }

    private Trip saveTrip(Vehicle vehicle, LocalDateTime departure, LocalDateTime arrivalOrigin,
                          Set<Collaborator> drivers) {
        LocalDateTime arrivalDestination = departure.plusHours(4);
        return tripRepository.saveAndFlush(new Trip(origin, destination,
                departure, arrivalDestination,
                arrivalOrigin.minusHours(4), arrivalOrigin,
                vehicle, drivers));
    }

    private Vehicle saveVehicle(String prefix, String licensePlate, VehicleStatus status) {
        return vehicleRepository.save(new Vehicle(
                prefix, licensePlate, BodyworkModel.MARCOPOLO,
                VehicleType.LD, 2023, 44, null, status));
    }

    private Collaborator saveDriver(String name, String cnhNumber) {
        return collaboratorRepository.save(new Collaborator(
                name, new Date(), Position.DRIVER, false,
                profileRepository.findByName("Administrador").orElseThrow(),
                null, null, new CNH(cnhNumber, CNHType.D), null));
    }
}
