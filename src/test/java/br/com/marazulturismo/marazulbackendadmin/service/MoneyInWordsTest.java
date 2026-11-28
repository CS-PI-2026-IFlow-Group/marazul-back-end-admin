package br.com.marazulturismo.marazulbackendadmin.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyInWordsTest {

    @ParameterizedTest
    @CsvSource({
            "0.00, zero reais",
            "0.01, um centavo",
            "0.50, cinquenta centavos",
            "1.00, um real",
            "2.00, dois reais",
            "1.01, um real e um centavo",
            "100.00, cem reais",
            "101.00, cento e um reais",
            "125.50, cento e vinte e cinco reais e cinquenta centavos",
            "1000.00, mil reais",
            "1001.00, mil e um reais",
            "1234.56, mil duzentos e trinta e quatro reais e cinquenta e seis centavos",
            "1000000.00, um milhão de reais",
            "2000000.00, dois milhões de reais"
    })
    void shouldConvertMoneyToWords(String amount, String expected) {
        assertEquals(
                expected,
                MoneyInWords.convert(new BigDecimal(amount))
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MoneyInWords.convert(null)
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MoneyInWords.convert(new BigDecimal("-1.00"))
        );
    }
}