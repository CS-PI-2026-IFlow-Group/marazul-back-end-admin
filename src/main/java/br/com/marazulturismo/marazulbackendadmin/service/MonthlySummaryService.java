package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.MonthlySummaryIndicatorDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.MonthlySummaryResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.MonthlyTopClientDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.exception.ReportPeriodValidationException;
import br.com.marazulturismo.marazulbackendadmin.repository.MonthlySummaryRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.MonthlySummaryRepository.MonthlyAggregate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MonthlySummaryService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM");
    private static final int MONEY_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 1;

    private final MonthlySummaryRepository monthlySummaryRepository;

    public MonthlySummaryService(MonthlySummaryRepository monthlySummaryRepository) {
        this.monthlySummaryRepository = monthlySummaryRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponseDTO generate(String monthParameter) {
        YearMonth month = parseMonth(monthParameter);
        YearMonth previousMonth = month.minusMonths(1);
        LocalDateTime queryStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime queryEnd = month.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();

        Map<YearMonth, MonthlyAggregate> aggregates = monthlySummaryRepository
                .summarizeMonths(queryStart, queryEnd).stream()
                .collect(Collectors.toMap(
                        aggregate -> YearMonth.of(aggregate.getAno(), aggregate.getMes()),
                        Function.identity()));

        MonthlyAggregate current = aggregates.get(month);
        MonthlyAggregate previous = aggregates.get(previousMonth);

        BigDecimal trips = countValue(current == null ? null : current.getViagens());
        BigDecimal previousTrips = countValue(previous == null ? null : previous.getViagens());
        BigDecimal passengers = countValue(current == null ? null : current.getPassageiros());
        BigDecimal previousPassengers = countValue(previous == null ? null : previous.getPassageiros());
        BigDecimal received = moneyValue(current == null ? null : current.getRecebido());
        BigDecimal previousReceived = moneyValue(previous == null ? null : previous.getRecebido());
        BigDecimal expenses = moneyValue(current == null ? null : current.getGastos());
        BigDecimal previousExpenses = moneyValue(previous == null ? null : previous.getGastos());
        BigDecimal result = received.subtract(expenses);
        BigDecimal previousResult = previousReceived.subtract(previousExpenses);

        List<MonthlySummaryIndicatorDTO> indicators = List.of(
                indicator("viagens", "Viagens concluídas", ReportValueFormat.NUMERO,
                        trips, previousTrips, 0),
                indicator("passageiros", "Passageiros transportados", ReportValueFormat.NUMERO,
                        passengers, previousPassengers, 0),
                indicator("recebido", "Recebido", ReportValueFormat.MOEDA,
                        received, previousReceived, MONEY_SCALE),
                indicator("gastos", "Gastos", ReportValueFormat.MOEDA,
                        expenses, previousExpenses, MONEY_SCALE),
                indicator("resultado", "Resultado", ReportValueFormat.MOEDA,
                        result, previousResult, MONEY_SCALE));

        var topClientAggregates = monthlySummaryRepository.findTopClients(monthStart, monthEnd);
        List<MonthlyTopClientDTO> topClients = IntStream.range(0, topClientAggregates.size())
                .mapToObj(index -> {
                    var client = topClientAggregates.get(index);
                    return new MonthlyTopClientDTO(index + 1, client.getClienteId(),
                            client.getCliente(), client.getViagens());
                })
                .toList();

        return new MonthlySummaryResponseDTO(
                MONTH_FORMAT.format(month),
                new ReportPeriodDTO(month.atDay(1), month.atEndOfMonth()),
                LocalDateTime.now(),
                indicators,
                topClients);
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || !value.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new ReportPeriodValidationException(
                    "Informe o mês no formato obrigatório yyyy-MM.");
        }
        return YearMonth.parse(value, MONTH_FORMAT);
    }

    private static MonthlySummaryIndicatorDTO indicator(String key, String title, ReportValueFormat format,
                                                         BigDecimal value, BigDecimal previousValue, int scale) {
        BigDecimal variation = value.subtract(previousValue).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal variationPercentage = previousValue.signum() == 0
                ? null
                : variation.divide(previousValue.abs(), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);

        return new MonthlySummaryIndicatorDTO(key, title, format,
                value.setScale(scale, RoundingMode.HALF_UP),
                previousValue.setScale(scale, RoundingMode.HALF_UP),
                variation, variationPercentage);
    }

    private static BigDecimal countValue(Long value) {
        return BigDecimal.valueOf(value == null ? 0L : value);
    }

    private static BigDecimal moneyValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE) : value;
    }
}
