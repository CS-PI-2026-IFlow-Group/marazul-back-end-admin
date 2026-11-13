package br.com.marazulturismo.marazulbackendadmin.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerListPdfServiceTest {

    @Test
    void formatCpf_formatsElevenDigitsAndKeepsAnythingElse() {
        assertThat(PassengerListPdfService.formatCpf("12345678900")).isEqualTo("123.456.789-00");
        assertThat(PassengerListPdfService.formatCpf("AB1234567")).isEqualTo("AB1234567");
        assertThat(PassengerListPdfService.formatCpf(null)).isNull();
    }

    @Test
    void formatPhone_formatsMobileAndLandlineAndFlagsMissingPhone() {
        assertThat(PassengerListPdfService.formatPhone("51999887766")).isEqualTo("(51) 99988-7766");
        assertThat(PassengerListPdfService.formatPhone("5133334444")).isEqualTo("(51) 3333-4444");
        assertThat(PassengerListPdfService.formatPhone("123")).isEqualTo("123");
        assertThat(PassengerListPdfService.formatPhone(null)).isEqualTo("Não informado");
    }
}
