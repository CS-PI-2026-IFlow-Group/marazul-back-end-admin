package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListItemDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListResponseDTO;
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
public class PassengerListPdfService {

    private static final String TEMPLATE = "reports/passengerList";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine templateEngine;
    private final String companyName;

    public PassengerListPdfService(TemplateEngine templateEngine,
                                   @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.templateEngine = templateEngine;
        this.companyName = companyName;
    }

    public byte[] render(PassengerListResponseDTO list) {
        String html = templateEngine.process(TEMPLATE, context(list));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF da lista de passageiros.", e);
        }
    }

    public String fileName(PassengerListResponseDTO list) {
        return "lista-passageiros-viagem-" + list.tripId() + ".pdf";
    }

    private Context context(PassengerListResponseDTO list) {
        Context context = new Context(BRAZIL);
        context.setVariable("company", companyName);
        context.setVariable("tripId", list.tripId());
        context.setVariable("route", list.originCity() + "/" + list.originState()
                + " para " + list.destinationCity() + "/" + list.destinationState());
        context.setVariable("departure", DATE_TIME.format(list.plannedDepartureOrigin()));
        context.setVariable("vehicle", list.vehiclePrefix() + " - " + list.vehicleLicensePlate());
        context.setVariable("drivers", String.join(", ", list.drivers()));
        context.setVariable("responsible", list.responsible() == null ? null : formatted(list.responsible()));
        context.setVariable("passengers", list.passengers().stream().map(PassengerListPdfService::formatted).toList());
        context.setVariable("total", list.totalPassengers());
        context.setVariable("issuedAt", DATE_TIME.format(list.issuedAt()));
        return context;
    }

    private static PassengerListItemDTO formatted(PassengerListItemDTO item) {
        return new PassengerListItemDTO(item.number(), item.passengerId(), item.name(),
                formatCpf(item.cpf()), formatPhone(item.phone()), item.responsible());
    }

    static String formatCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    static String formatPhone(String phone) {
        if (phone == null) {
            return "Não informado";
        }
        if (phone.matches("\\d{11}")) {
            return "(" + phone.substring(0, 2) + ") " + phone.substring(2, 7) + "-" + phone.substring(7);
        }
        if (phone.matches("\\d{10}")) {
            return "(" + phone.substring(0, 2) + ") " + phone.substring(2, 6) + "-" + phone.substring(6);
        }
        return phone;
    }
}
