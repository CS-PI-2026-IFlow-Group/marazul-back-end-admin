package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerReceiptResponseDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PassengerReceiptPdfService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final TemplateEngine templateEngine;

    public PassengerReceiptPdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(List<PassengerReceiptResponseDTO> receipts) {
        if (receipts.isEmpty()) {
            throw new IllegalArgumentException("É necessário ao menos um recibo para gerar o PDF.");
        }
        Context context = new Context(BRAZIL);
        NumberFormat currency = NumberFormat.getCurrencyInstance(BRAZIL);
        context.setVariable("receipts", receipts.stream().map(receipt -> Map.of(
                "data", receipt,
                "cpf", PassengerListPdfService.formatCpf(receipt.passengerCpf()),
                "amount", currency.format(receipt.amountPaid()),
                "departure", DATE_TIME.format(receipt.plannedDepartureOrigin()),
                "issuedAt", DATE_TIME.format(receipt.issuedAt()))).toList());
        String html = templateEngine.process("reports/passengerReceipt", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gerar o PDF do recibo.", exception);
        }
    }
}
