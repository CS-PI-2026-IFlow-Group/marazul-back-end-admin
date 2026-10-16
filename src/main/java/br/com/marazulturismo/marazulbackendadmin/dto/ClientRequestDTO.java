package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ClientRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        String name,

        @Pattern(
                regexp = "^$|^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$",
                message = "CPF inválido. Informe no formato 000.000.000-00 ou somente dígitos.")
        String cpf,

        @Pattern(
                regexp = "^$|^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$",
                message = "CNPJ inválido. Informe no formato 00.000.000/0000-00 ou somente dígitos.")
        String cnpj,

        @NotNull(message = "O endereço é obrigatório.")
        @Valid
        AddressRequestDTO address
) {

    public static String normalizeDocument(String document) {
        if (document == null || document.isBlank()) {
            return null;
        }
        return document.replaceAll("\\D", "");
    }
}
