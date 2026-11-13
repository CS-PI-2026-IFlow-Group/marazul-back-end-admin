package br.com.marazulturismo.marazulbackendadmin.dto;

public record PassengerListItemDTO(
        int number,
        Long passengerId,
        String name,
        String cpf,
        String phone,
        boolean responsible
) {
}
