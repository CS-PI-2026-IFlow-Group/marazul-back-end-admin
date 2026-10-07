package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Address;

public record AddressResponseDTO(
        Long id,
        String street,
        String number,
        String complement,
        Long cityId,
        String city,
        String state
) {

    public static AddressResponseDTO fromEntity(Address address) {
        return new AddressResponseDTO(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getCity().getId(),
                address.getCity().getName(),
                address.getCity().getState().getAcronym());
    }
}
