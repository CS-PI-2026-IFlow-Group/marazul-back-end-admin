package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Locale;

public record VehicleRequestDTO(

        @NotBlank(message = "O prefixo é obrigatório.")
        String prefix,

        @NotBlank(message = "A placa é obrigatória.")
        @Pattern(
                regexp = "^[A-Za-z]{3}-?[0-9][A-Za-z0-9][0-9]{2}$",
                message = "A placa deve estar no formato ABC1234, ABC-1234 ou ABC1D23.")
        String licensePlate,

        @NotNull(message = "O modelo de carroceria é obrigatório.")
        BodyworkModel model,

        @NotNull(message = "O tipo é obrigatório.")
        VehicleType type,

        @NotNull(message = "O ano é obrigatório.")
        @Min(value = 1950, message = "O ano deve ser igual ou posterior a 1950.")
        Integer year,

        @NotNull(message = "A quantidade de assentos é obrigatória.")
        @Positive(message = "A quantidade de assentos deve ser maior que zero.")
        Integer seats,

        LocalDate inspectionDate,

        VehicleStatus status
) {

    public VehicleRequestDTO {
        licensePlate = normalizeLicensePlate(licensePlate);
    }

    public static String normalizeLicensePlate(String licensePlate) {
        if (licensePlate == null) {
            return null;
        }
        return licensePlate.replaceAll("[\s-]", "").toUpperCase(Locale.ROOT);
    }
}
