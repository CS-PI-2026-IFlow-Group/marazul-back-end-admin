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
        AddressResponseDTO addressDto = client.getAddress() == null
                ? null : AddressResponseDTO.fromEntity(client.getAddress());

        return new ClientDetailResponseDTO(
                client.getId(),
                client.getName(),
                client.getCpf(),
                client.getCnpj(),
                addressDto
        );
    }
}
