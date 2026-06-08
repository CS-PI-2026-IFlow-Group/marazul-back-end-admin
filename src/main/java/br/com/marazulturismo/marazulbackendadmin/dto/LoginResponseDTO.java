package br.com.marazulturismo.marazulbackendadmin.dto;

public record LoginResponseDTO(
        String token,
        Long id,
        String nome,
        String email
) {}
