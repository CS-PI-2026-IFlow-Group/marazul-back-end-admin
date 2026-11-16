package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ServiceOrderResponseDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ServiceOrderPdfService {

    private static final String TEMPLATE = "reports/serviceOrder";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine templateEngine;
    private final String companyName;

    public ServiceOrderPdfService(TemplateEngine templateEngine,
                                  @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.templateEngine = templateEngine;
        this.companyName = companyName;
    }

    public byte[] render(ServiceOrderResponseDTO order) {
        Context context = new Context(BRAZIL);
        context.setVariable("company", companyName);
        context.setVariable("order", order);
        context.setVariable("departureOrigin", format(order.plannedDepartureOrigin()));
        context.setVariable("arrivalDestination", format(order.plannedArrivalDestination()));
        context.setVariable("departureDestination", format(order.plannedDepartureDestination()));
        context.setVariable("arrivalOrigin", format(order.plannedArrivalOrigin()));
        context.setVariable("drivers", String.join(", ", order.drivers()));
        context.setVariable("responsiblePassengerPhone", formatPhone(order.responsiblePassengerPhone()));
        context.setVariable("notesFontSize", notesFontSize(order.notes()));
        String html = templateEngine.process(TEMPLATE, context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF da ordem de serviço.", e);
        }
    }

    public String fileName(ServiceOrderResponseDTO order) {
        return "ordem-servico-viagem-" + order.tripId() + ".pdf";
    }

    private static String format(LocalDateTime dateTime) {
        return DATE_TIME.format(dateTime);
    }

    private static String formatPhone(String phone) {
        if (phone == null) {
            return "";
        }
        if (phone.matches("\\d{11}")) {
            return "(" + phone.substring(0, 2) + ") " + phone.substring(2, 7) + "-" + phone.substring(7);
        }
        if (phone.matches("\\d{10}")) {
            return "(" + phone.substring(0, 2) + ") " + phone.substring(2, 6) + "-" + phone.substring(6);
        }
        return phone;
    }

    private static double notesFontSize(String notes) {
        if (notes == null || notes.isBlank()) {
            return 10.0;
        }
        return Math.max(1.5, Math.min(10.0, Math.sqrt(100_000.0 / notes.length())));
    }
}
