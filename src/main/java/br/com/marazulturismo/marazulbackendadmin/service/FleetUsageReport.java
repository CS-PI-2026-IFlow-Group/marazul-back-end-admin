package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripVehiclePeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.model.Vehicle;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FleetUsageReport implements ReportDefinition {

    private static final List<ReportColumnDTO> COLUMNS = List.of(
            ReportColumnDTO.text("prefixo", "Prefixo"),
            ReportColumnDTO.text("placa", "Placa"),
            ReportColumnDTO.centered("situacao", "Situação"),
            ReportColumnDTO.number("viagens", "Viagens"),
            ReportColumnDTO.number("diasRodados", "Dias rodados"),
            ReportColumnDTO.number("diasParados", "Dias parados"),
            ReportColumnDTO.percent("aproveitamento", "Aproveitamento"));

    private static final Comparator<VehicleUsage> BY_PREFIX =
            Comparator.comparing(it -> it.vehicle().getPrefix());

    private static final Comparator<VehicleUsage> MOST_USED_FIRST =
            Comparator.comparingInt(VehicleUsage::workedDays).reversed().thenComparing(BY_PREFIX);

    private static final Comparator<VehicleUsage> LEAST_USED_FIRST =
            Comparator.comparingInt(VehicleUsage::workedDays).thenComparing(BY_PREFIX);

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;

    public FleetUsageReport(VehicleRepository vehicleRepository, TripRepository tripRepository) {
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    public String slug() {
        return "uso-frota";
    }

    @Override
    public String title() {
        return "Uso dos ônibus";
    }

    @Override
    public List<ReportColumnDTO> columns() {
        return COLUMNS;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportContentDTO content(ReportPeriodDTO period) {
        int periodDays = (int) ChronoUnit.DAYS.between(period.dataInicio(), period.dataFim()) + 1;
        List<TripVehiclePeriodDTO> trips =
                tripRepository.findVehiclePeriodsOverlapping(period.start(), period.exclusiveEnd());

        Map<Long, Set<LocalDate>> workedDaysByVehicle = new HashMap<>();
        Map<Long, Integer> tripsByVehicle = new HashMap<>();

        for (TripVehiclePeriodDTO trip : trips) {
            LocalDate firstDay = latest(trip.plannedDepartureOrigin().toLocalDate(), period.dataInicio());
            LocalDate lastDay = earliest(trip.plannedArrivalOrigin().toLocalDate(), period.dataFim());
            Set<LocalDate> workedDays = workedDaysByVehicle.computeIfAbsent(trip.vehicleId(), id -> new HashSet<>());
            firstDay.datesUntil(lastDay.plusDays(1)).forEach(workedDays::add);

            if (!trip.plannedDepartureOrigin().isBefore(period.start())) {
                tripsByVehicle.merge(trip.vehicleId(), 1, Integer::sum);
            }
        }

        List<VehicleUsage> usages = vehicleRepository.findAll().stream()
                .map(vehicle -> new VehicleUsage(
                        vehicle,
                        tripsByVehicle.getOrDefault(vehicle.getId(), 0),
                        workedDaysByVehicle.getOrDefault(vehicle.getId(), Set.of()).size()))
                .sorted(MOST_USED_FIRST)
                .toList();

        List<Map<String, Object>> rows = usages.stream()
                .map(usage -> row(usage, periodDays))
                .toList();

        return ReportContentDTO.of(rows, totals(usages, !trips.isEmpty()));
    }

    private static Map<String, Object> row(VehicleUsage usage, int periodDays) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("veiculoId", usage.vehicle().getId());
        row.put("prefixo", usage.vehicle().getPrefix());
        row.put("placa", usage.vehicle().getLicensePlate());
        row.put("situacao", usage.vehicle().getStatus().getLabel());
        row.put("viagens", usage.trips());
        row.put("diasRodados", usage.workedDays());
        row.put("diasParados", periodDays - usage.workedDays());
        row.put("aproveitamento", ratio(usage.workedDays() * 100, periodDays));
        return row;
    }

    private static List<ReportTotalDTO> totals(List<VehicleUsage> usages, boolean hasTrips) {
        int totalTrips = usages.stream().mapToInt(VehicleUsage::trips).sum();
        int totalWorkedDays = usages.stream().mapToInt(VehicleUsage::workedDays).sum();
        BigDecimal averageWorkedDays = usages.isEmpty()
                ? BigDecimal.ZERO.setScale(1)
                : ratio(totalWorkedDays, usages.size());

        List<VehicleUsage> activeUsages = hasTrips
                ? usages.stream().filter(it -> it.vehicle().getStatus() == VehicleStatus.ACTIVE).toList()
                : List.of();

        return List.of(
                new ReportTotalDTO("viagens", "Viagens no período", ReportValueFormat.NUMERO, totalTrips),
                new ReportTotalDTO("mediaDiasRodados", "Média de dias rodados",
                        ReportValueFormat.NUMERO, averageWorkedDays),
                new ReportTotalDTO("maisUtilizado", "Ônibus mais utilizado",
                        ReportValueFormat.TEXTO, firstPrefix(activeUsages, MOST_USED_FIRST)),
                new ReportTotalDTO("menosUtilizado", "Ônibus menos utilizado",
                        ReportValueFormat.TEXTO, firstPrefix(activeUsages, LEAST_USED_FIRST)));
    }

    private static String firstPrefix(List<VehicleUsage> usages, Comparator<VehicleUsage> order) {
        return usages.stream()
                .min(order)
                .map(it -> it.vehicle().getPrefix())
                .orElse(null);
    }

    private static BigDecimal ratio(int dividend, int divisor) {
        return BigDecimal.valueOf(dividend).divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP);
    }

    private static LocalDate latest(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDate earliest(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private record VehicleUsage(Vehicle vehicle, int trips, int workedDays) {
    }
}
