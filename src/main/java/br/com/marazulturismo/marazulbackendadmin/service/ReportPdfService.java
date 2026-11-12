package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTableResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportPdfService {

    private static final String TEMPLATE = "reports/report";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine templateEngine;
    private final String companyName;

    public ReportPdfService(TemplateEngine templateEngine,
                            @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.templateEngine = templateEngine;
        this.companyName = companyName;
    }

    public byte[] render(ReportTableResponseDTO report) {
        String html = templateEngine.process(TEMPLATE, context(report));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF do relatório.", e);
        }
    }

    public String fileName(ReportTableResponseDTO report) {
        StringBuilder name = new StringBuilder("relatorio-").append(report.relatorio());

        if (report.periodo() == null) {
            name.append("-").append(FILE_DATE.format(report.emitidoEm()));
        } else {
            name.append("-").append(FILE_DATE.format(report.periodo().dataInicio()))
                    .append("-a-").append(FILE_DATE.format(report.periodo().dataFim()));
        }

        return name.append(".pdf").toString();
    }

    private Context context(ReportTableResponseDTO report) {
        Context context = new Context(BRAZIL);
        context.setVariable("empresa", companyName);
        context.setVariable("titulo", report.titulo());
        context.setVariable("emitidoEm", DATE_TIME.format(report.emitidoEm()));
        context.setVariable("periodo", period(report));
        context.setVariable("colunas", columns(report.colunas()));
        context.setVariable("linhas", rows(report));
        context.setVariable("totais", totals(report.totais()));
        return context;
    }

    private static String period(ReportTableResponseDTO report) {
        if (report.periodo() == null) {
            return null;
        }
        return DATE.format(report.periodo().dataInicio()) + " a " + DATE.format(report.periodo().dataFim());
    }

    private static List<Map<String, String>> columns(List<ReportColumnDTO> columns) {
        List<Map<String, String>> rendered = new ArrayList<>();
        for (ReportColumnDTO column : nullSafe(columns)) {
            Map<String, String> cell = new LinkedHashMap<>();
            cell.put("titulo", column.titulo());
            cell.put("alinhamento", column.alinhamento().name().toLowerCase(Locale.ROOT));
            rendered.add(cell);
        }
        return rendered;
    }

    private static List<List<String>> rows(ReportTableResponseDTO report) {
        List<List<String>> rendered = new ArrayList<>();
        for (Map<String, Object> row : nullSafe(report.linhas())) {
            List<String> cells = new ArrayList<>();
            for (ReportColumnDTO column : nullSafe(report.colunas())) {
                cells.add(format(row.get(column.chave())));
            }
            rendered.add(cells);
        }
        return rendered;
    }

    private static List<Map<String, String>> totals(List<ReportTotalDTO> totals) {
        List<Map<String, String>> rendered = new ArrayList<>();
        for (ReportTotalDTO total : nullSafe(totals)) {
            Map<String, String> cell = new LinkedHashMap<>();
            cell.put("titulo", total.titulo());
            cell.put("valor", format(total.valor()));
            rendered.add(cell);
        }
        return rendered;
    }

    private static String format(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDate date) {
            return DATE.format(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME.format(dateTime);
        }
        if (value instanceof BigDecimal decimal) {
            return String.format(BRAZIL, "%,.2f", decimal);
        }
        if (value instanceof Boolean flag) {
            return flag ? "Sim" : "Não";
        }
        return String.valueOf(value);
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
