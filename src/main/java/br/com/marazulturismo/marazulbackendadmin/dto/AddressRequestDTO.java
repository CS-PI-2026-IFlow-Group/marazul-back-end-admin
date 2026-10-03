package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequestDTO(

        @NotBlank(message = "A rua é obrigatória.")
        String street,

        @NotBlank(message = "O número é obrigatório.")
        String number,

        String complement,

        @NotBlank(message = "A cidade é obrigatória.")
        String city,

        @NotBlank(message = "O estado (UF) é obrigatório.")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "O estado deve conter exatamente 2 letras (sigla UF).")
        String state
) {
}
