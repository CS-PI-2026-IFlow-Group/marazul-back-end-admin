package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequestDTO(

        @NotBlank(message = "A rua é obrigatória.")
        String street,

        @NotBlank(message = "O número é obrigatório.")
        String number,

        String complement,

        String city,

        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado deve conter exatamente 2 letras (sigla UF).")
        String state,

        Long cityId
) {
    @AssertTrue(message = "Informe o ID da cidade ou a cidade e a UF.")
    public boolean isCityReferenceValid() {
        return cityId != null || (city != null && !city.isBlank() && state != null && !state.isBlank());
    }
}
