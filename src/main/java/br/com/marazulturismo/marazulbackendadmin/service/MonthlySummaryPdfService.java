package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.MonthlySummaryIndicatorDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.MonthlySummaryResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class MonthlySummaryPdfService {

    private static final String TEMPLATE = "reports/monthlySummary";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine templateEngine;
    private final String companyName;

    public MonthlySummaryPdfService(TemplateEngine templateEngine,
                                    @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.templateEngine = templateEngine;
        this.companyName = companyName;
    }

    public byte[] render(MonthlySummaryResponseDTO summary) {
        String html = templateEngine.process(TEMPLATE, context(summary));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gerar o PDF do resumo mensal.", exception);
        }
    }

    public String fileName(MonthlySummaryResponseDTO summary) {
        return "resumo-mensal-" + summary.mes() + ".pdf";
    }

    private Context context(MonthlySummaryResponseDTO summary) {
        Context context = new Context(BRAZIL);
        context.setVariable("company", companyName);
        context.setVariable("month", summary.mes());
        context.setVariable("period", DATE.format(summary.periodo().dataInicio())
                + " a " + DATE.format(summary.periodo().dataFim()));
        context.setVariable("issuedAt", DATE_TIME.format(summary.emitidoEm()));
        context.setVariable("indicators", indicators(summary.indicadores()));
        context.setVariable("topClients", summary.principaisClientes());
        return context;
    }

    private static List<IndicatorView> indicators(List<MonthlySummaryIndicatorDTO> indicators) {
        return indicators.stream()
                .map(indicator -> new IndicatorView(
                        indicator.titulo(),
                        format(indicator.valor(), indicator.formato()),
                        format(indicator.valorAnterior(), indicator.formato()),
                        format(indicator.variacao(), indicator.formato()),
                        format(indicator.variacaoPercentual(), ReportValueFormat.PERCENTUAL)))
                .toList();
    }

    private static String format(Object value, ReportValueFormat format) {
        return ReportPdfService.format(value, format);
    }

    private record IndicatorView(
            String title,
            String value,
            String previousValue,
            String variation,
            String variationPercentage
    ) {
    }
}
