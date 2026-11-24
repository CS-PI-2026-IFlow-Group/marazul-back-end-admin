package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WeeklyScheduleReport implements ReportDefinition {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String NO_TRIP = "Sem viagem";

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public WeeklyScheduleReport(TripRepository tripRepository, VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public String slug() {
        return "agenda";
    }

    @Override
    public String title() {
        return "Agenda da semana";
    }

    @Override
    public List<ReportColumnDTO> columns() {
        return List.of(
                ReportColumnDTO.centered("dia", "Dia"),
                ReportColumnDTO.text("diaSemana", "Dia da semana"),
                ReportColumnDTO.centered("saida", "Saída"),
                ReportColumnDTO.text("trecho", "Trecho"),
                ReportColumnDTO.text("onibus", "Ônibus"),
                ReportColumnDTO.text("motoristas", "Motoristas"),
                ReportColumnDTO.centered("situacao", "Situação"));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportContentDTO content(ReportPeriodDTO period) {
        LocalDateTime start = period.start();
        LocalDateTime end = period.exclusiveEnd();

        Map<LocalDate, List<Trip>> tripsByDay = tripRepository.findScheduleBetween(start, end).stream()
                .collect(Collectors.groupingBy(trip -> trip.getPlannedDepartureOrigin().toLocalDate()));

        Collator collator = Collator.getInstance(BRAZIL);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate day = period.dataInicio(); !day.isAfter(period.dataFim()); day = day.plusDays(1)) {
            List<Trip> trips = tripsByDay.getOrDefault(day, List.of());
            if (trips.isEmpty()) {
                rows.add(emptyDayRow(day));
            }
            for (Trip trip : trips) {
                rows.add(tripRow(trip, collator));
            }
        }

        List<ReportTotalDTO> totals = List.of(
                new ReportTotalDTO("viagens", "Viagens",
                        tripRepository.countDepartingBetween(start, end)),
                new ReportTotalDTO("onibusEnvolvidos", "Ônibus envolvidos",
                        tripRepository.countVehiclesDepartingBetween(start, end)),
                new ReportTotalDTO("onibusSemViagem", "Ônibus sem viagem",
                        vehicleRepository.findPrefixesWithoutTripBetween(VehicleStatus.ACTIVE, start, end)));

        return ReportContentDTO.of(rows, totals);
    }

    private static Map<String, Object> emptyDayRow(LocalDate day) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("viagemId", null);
        row.put("dia", day);
        row.put("diaSemana", weekday(day));
        row.put("saida", null);
        row.put("trecho", null);
        row.put("onibus", null);
        row.put("motoristas", null);
        row.put("situacao", NO_TRIP);
        return row;
    }

    private static Map<String, Object> tripRow(Trip trip, Collator collator) {
        LocalDateTime departure = trip.getPlannedDepartureOrigin();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("viagemId", trip.getId());
        row.put("dia", departure.toLocalDate());
        row.put("diaSemana", weekday(departure.toLocalDate()));
        row.put("saida", TIME.format(departure));
        row.put("trecho", place(trip.getOriginCity()) + " → " + place(trip.getDestinationCity()));
        row.put("onibus", trip.getVehicle().getPrefix());
        row.put("motoristas", drivers(trip, collator));
        row.put("situacao", trip.getStatus().getLabel());
        return row;
    }

    private static String weekday(LocalDate day) {
        String name = day.getDayOfWeek().getDisplayName(TextStyle.FULL, BRAZIL);
        return name.substring(0, 1).toUpperCase(BRAZIL) + name.substring(1);
    }

    private static String place(City city) {
        return city.getName() + "/" + city.getState().getAcronym();
    }

    private static String drivers(Trip trip, Collator collator) {
        List<String> names = trip.getDrivers().stream()
                .map(Collaborator::getName)
                .sorted(collator)
                .toList();
        return names.isEmpty() ? null : String.join(", ", names);
    }
}
