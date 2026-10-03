package br.com.marazulturismo.marazulbackendadmin.dto;

public record AddressResponseDTO(
        String street,
        String number,
        String complement,
        String city,
        String state
) {
}
