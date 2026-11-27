package br.com.marazulturismo.marazulbackendadmin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyInWords {

    private static final String[] UNITS = {
            "", "um", "dois", "três", "quatro", "cinco",
            "seis", "sete", "oito", "nove"
    };

    private static final String[] TEENS = {
            "dez", "onze", "doze", "treze", "quatorze",
            "quinze", "dezesseis", "dezessete",
            "dezoito", "dezenove"
    };

    private static final String[] TENS = {
            "", "", "vinte", "trinta", "quarenta",
            "cinquenta", "sessenta", "setenta",
            "oitenta", "noventa"
    };

    private static final String[] HUNDREDS = {
            "", "cento", "duzentos", "trezentos",
            "quatrocentos", "quinhentos", "seiscentos",
            "setecentos", "oitocentos", "novecentos"
    };

    private MoneyInWords() {
    }

    public static String convert(BigDecimal amount) {

        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor monetário inválido."
            );
        }

        BigDecimal rounded = amount.setScale(
                2, RoundingMode.HALF_UP
        );

        long reais = rounded.toBigInteger().longValueExact();

        int centavos = rounded.remainder(BigDecimal.ONE)
                .movePointRight(2)
                .intValueExact();

        if (reais == 0 && centavos == 0) {
            return "zero reais";
        }

        StringBuilder result = new StringBuilder();

        if (reais > 0) {
            result.append(numberToWords(reais));

            if (reais == 1) {
                result.append(" real");
            } else if (reais % 1_000_000 == 0) {
                result.append(" de reais");
            } else {
                result.append(" reais");
            }
        }

        if (centavos > 0) {
            if (reais > 0) {
                result.append(" e ");
            }

            result.append(numberToWords(centavos));
            result.append(
                    centavos == 1 ? " centavo" : " centavos"
            );
        }

        return result.toString();
    }

    private static String numberToWords(long number) {

        if (number < 1000) {
            return belowThousand((int) number);
        }

        if (number < 1_000_000) {
            long thousands = number / 1000;
            long remainder = number % 1000;

            String prefix = thousands == 1
                    ? "mil"
                    : numberToWords(thousands) + " mil";

            return join(prefix, remainder);
        }

        if (number < 1_000_000_000) {
            long millions = number / 1_000_000;
            long remainder = number % 1_000_000;

            String prefix = numberToWords(millions)
                    + (millions == 1 ? " milhão" : " milhões");

            return join(prefix, remainder);
        }

        if (number < 1_000_000_000_000L) {
            long billions = number / 1_000_000_000;
            long remainder = number % 1_000_000_000;

            String prefix = numberToWords(billions)
                    + (billions == 1 ? " bilhão" : " bilhões");

            return join(prefix, remainder);
        }

        throw new IllegalArgumentException(
                "Valor monetário acima do limite suportado."
        );
    }

    private static String belowThousand(int number) {

        if (number == 100) {
            return "cem";
        }

        if (number < 10) {
            return UNITS[number];
        }

        if (number < 20) {
            return TEENS[number - 10];
        }

        if (number < 100) {
            int tens = number / 10;
            int units = number % 10;

            return TENS[tens]
                    + (units == 0 ? "" : " e " + UNITS[units]);
        }

        int hundreds = number / 100;
        int remainder = number % 100;

        return HUNDREDS[hundreds]
                + (remainder == 0
                ? ""
                : " e " + belowThousand(remainder));
    }

    private static String join(String prefix, long remainder) {

        if (remainder == 0) {
            return prefix;
        }

        String separator = remainder < 100
                || (remainder < 1000 && remainder % 100 == 0)
                ? " e "
                : " ";

        return prefix + separator + numberToWords(remainder);
    }
}
