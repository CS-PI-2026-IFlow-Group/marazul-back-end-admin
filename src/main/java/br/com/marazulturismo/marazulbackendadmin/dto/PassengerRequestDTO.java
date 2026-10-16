package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PassengerRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        String name,

        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(
                regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$",
                message = "CPF inválido. Informe no formato 000.000.000-00 ou somente dígitos.")
        String cpf,

        String phone
) {
}
