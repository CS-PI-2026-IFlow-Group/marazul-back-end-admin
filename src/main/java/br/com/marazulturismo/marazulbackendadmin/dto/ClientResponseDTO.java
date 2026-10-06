package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Client;

public record ClientResponseDTO(
        Long id,
        String name,
        String cpf,
        String cnpj,
        AddressResponseDTO address
) {

    public static ClientResponseDTO fromEntity(Client client) {
        return new ClientResponseDTO(
                client.getId(),
                client.getName(),
                client.getCpf(),
                client.getCnpj(),
                AddressResponseDTO.fromEntity(client.getAddress()));
    }
}
