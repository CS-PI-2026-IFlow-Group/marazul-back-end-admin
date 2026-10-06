package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Client;

public record ClientDetailResponseDTO(
        Long id,
        String name,
        String cpf,
        String cnpj,
        AddressResponseDTO address
) {

    public static ClientDetailResponseDTO fromEntity(Client client) {
        AddressResponseDTO addressDto = null;

        if (client.getAddress() != null) {
            addressDto = new AddressResponseDTO(
                    client.getAddress().getStreet(),
                    client.getAddress().getNumber(),
                    client.getAddress().getComplement(),
                    client.getAddress().getCity().getName(),
                    client.getAddress().getCity().getState().getAcronym()
            );
        }

        return new ClientDetailResponseDTO(
                client.getId(),
                client.getName(),
                client.getCpf(),
                client.getCnpj(),
                addressDto
        );
    }
}
