package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Passenger;

public record PassengerResponseDTO(
        Long id,
        String name,
        String cpf,
        String phone
) {

    public static PassengerResponseDTO fromEntity(Passenger passenger) {
        return new PassengerResponseDTO(
                passenger.getId(),
                passenger.getName(),
                passenger.getCpf(),
                passenger.getPhone());
    }
}
