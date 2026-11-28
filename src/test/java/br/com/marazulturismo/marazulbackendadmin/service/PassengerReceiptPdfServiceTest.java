package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerReceiptResponseDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerReceiptPdfServiceTest {

    @ParameterizedTest
    @CsvSource({
            "1.005, '1,01', um real e um centavo",
            "0.005, '0,01', um centavo",
            "1.015, '1,02', um real e dois centavos",
            "0.995, '1,00', um real"
    })
    void render_keepsNumericAmountAndWordsConsistentWhenRounding(
            String amount, String expectedNumeric, String expectedWords) throws Exception {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        BigDecimal paid = new BigDecimal(amount);
        LocalDateTime date = LocalDateTime.of(2026, 12, 10, 8, 0);
        var receipt = new PassengerReceiptResponseDTO(
                "REC-10-20", 10L, 20L, "Ana Passageira", "11144477735",
                paid, MoneyInWords.convert(paid), "Porto Alegre", "RS",
                "Florianópolis", "SC", date, "Marazul Turismo", date);

        byte[] pdf = new PassengerReceiptPdfService(engine).render(List.of(receipt));
        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document)
                    .replace('\u00a0', ' ')
                    .replaceAll("\\s+", " ");
            assertThat(text).contains("R$ " + expectedNumeric + " (" + expectedWords + ")");
        }
    }
}
