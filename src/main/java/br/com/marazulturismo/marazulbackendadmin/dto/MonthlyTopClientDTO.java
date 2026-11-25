package br.com.marazulturismo.marazulbackendadmin.dto;

public record MonthlyTopClientDTO(
        Integer posicao,
        Long clienteId,
        String cliente,
        Long viagens
) {
}
