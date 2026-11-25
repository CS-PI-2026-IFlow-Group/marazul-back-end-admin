package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportPdfServiceTest {

    @Test
    void formatsValuesAccordingToColumnFormat() {
        assertEquals("R$ 1.250,50", ReportPdfService.format(
                new BigDecimal("1250.50"), ReportValueFormat.MOEDA));
        assertEquals("-R$ 1.250,50", ReportPdfService.format(
                new BigDecimal("-1250.50"), ReportValueFormat.MOEDA));
        assertEquals("R$ 1,01", ReportPdfService.format(
                new BigDecimal("1.005"), ReportValueFormat.MOEDA));
        assertEquals("75,0%", ReportPdfService.format(
                new BigDecimal("75.0"), ReportValueFormat.PERCENTUAL));
        assertEquals("23/11/2026", ReportPdfService.format(
                LocalDate.of(2026, 11, 23), ReportValueFormat.DATA));
        assertEquals("23/11/2026 14:05", ReportPdfService.format(
                LocalDateTime.of(2026, 11, 23, 14, 5), ReportValueFormat.DATA_HORA));
        assertEquals("980", ReportPdfService.format(
                new BigDecimal("980.00"), ReportValueFormat.NUMERO));
    }

    @Test
    void separatesListValuesWithCommas() {
        assertEquals("T004, T007", ReportPdfService.format(
                List.of("T004", "T007"), ReportValueFormat.TEXTO));
        assertEquals("R$ 1.000,00, R$ 2.000,00", ReportPdfService.format(
                List.of(new BigDecimal("1000"), new BigDecimal("2000")), ReportValueFormat.MOEDA));
        assertEquals("25,0%, 75,0%", ReportPdfService.format(
                new BigDecimal[]{new BigDecimal("25"), new BigDecimal("75")}, ReportValueFormat.PERCENTUAL));
        assertEquals("-", ReportPdfService.format(Collections.emptyList(), ReportValueFormat.TEXTO));
        assertEquals("-", ReportPdfService.format(new String[0], ReportValueFormat.TEXTO));
        assertEquals("-", ReportPdfService.format(null, ReportValueFormat.MOEDA));
    }
}
