package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.Client;

public record AddressResponseDTO(
        Long id,
        String street,
        String number,
        String complement,
        Long cityId,
        String cityName,
        Long stateId,
        String stateName,
        String stateAcronym,
        Long clientId,
        String clientName
) {

    public static AddressResponseDTO fromEntity(Address address, Client client) {
        return new AddressResponseDTO(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getCity().getId(),
                address.getCity().getName(),
                address.getCity().getState().getId(),
                address.getCity().getState().getName(),
                address.getCity().getState().getAcronym(),
                client != null ? client.getId() : null,
                client != null ? client.getName() : null);
    }
}
