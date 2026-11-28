package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripResultResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TripResultPdfService {

    private static final String TEMPLATE = "reports/tripResult";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine templateEngine;
    private final String companyName;

    public TripResultPdfService(TemplateEngine templateEngine,
                                @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.templateEngine = templateEngine;
        this.companyName = companyName;
    }

    public byte[] render(TripResultResponseDTO result) {
        String html = templateEngine.process(TEMPLATE, context(result));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF do resultado da viagem.", e);
        }
    }

    public String fileName(TripResultResponseDTO result) {
        return "resultado-viagem-" + result.viagemId() + ".pdf";
    }

    private Context context(TripResultResponseDTO result) {
        Context context = new Context(BRAZIL);
        context.setVariable("empresa", companyName);
        context.setVariable("viagemId", result.viagemId());
        context.setVariable("trecho", result.trecho());
        context.setVariable("saida", DATE_TIME.format(result.dataSaida()));
        context.setVariable("cliente", result.cliente() == null ? "Não informado" : result.cliente());
        context.setVariable("situacao", result.situacao());
        context.setVariable("valores", List.of(
                value("Cobrado", result.cobrado()),
                value("Recebido", result.recebido()),
                value("Gastos", result.gastos())));
        context.setVariable("resultado", money(result.resultado()));
        context.setVariable("prejuizo", result.resultado().signum() < 0);
        context.setVariable("gastos", result.gastosDetalhados().stream()
                .map(expense -> Map.of("descricao", expense.descricao(), "valor", money(expense.valor())))
                .toList());
        context.setVariable("emitidoEm", DATE_TIME.format(result.emitidoEm()));
        return context;
    }

    private static Map<String, String> value(String title, BigDecimal amount) {
        return Map.of("titulo", title, "valor", money(amount));
    }

    private static String money(BigDecimal amount) {
        return ReportPdfService.format(amount, ReportValueFormat.MOEDA);
    }
}
