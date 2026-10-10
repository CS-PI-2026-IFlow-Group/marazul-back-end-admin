package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.Client;

public record AddressResponseDTO(
        Long id,
        String street,
        String number,
        String complement,
        Long cityId,
        String city,
        String cityName,
        Long stateId,
        String stateName,
        String state,
        String stateAcronym,
        Long clientId,
        String clientName
) {

    public static AddressResponseDTO fromEntity(Address address) {
        return fromEntity(address, null);
    }

    public static AddressResponseDTO fromEntity(Address address, Client client) {
        if (address == null) {
            return null;
        }

        Long cityId = address.getCity() != null ? address.getCity().getId() : null;
        String cityName = address.getCity() != null ? address.getCity().getName() : null;
        Long stateId = address.getCity() != null && address.getCity().getState() != null
                ? address.getCity().getState().getId() : null;
        String stateName = address.getCity() != null && address.getCity().getState() != null
                ? address.getCity().getState().getName() : null;
        String stateAcronym = address.getCity() != null && address.getCity().getState() != null
                ? address.getCity().getState().getAcronym() : null;

        return new AddressResponseDTO(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                cityId,
                cityName,
                cityName,
                stateId,
                stateName,
                stateAcronym,
                stateAcronym,
                client != null ? client.getId() : null,
                client != null ? client.getName() : null
        );
    }
}
