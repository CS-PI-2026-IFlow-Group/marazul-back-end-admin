package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Client;

public record ClientListResponseDTO(
        Long id,
        String name,
        String cpf,
        String cnpj,
        String city,
        String state
) {
    public static ClientListResponseDTO fromEntity(Client client) {
        String city = null;
        String state = null;

        if (client.getAddress() != null) {
            city = client.getAddress().getCity().getName();
            state = client.getAddress().getCity().getState().getAcronym();
        }

        return new ClientListResponseDTO(
                client.getId(),
                client.getName(),
                client.getCpf(),
                client.getCnpj(),
                city,
                state
        );
    }
}
