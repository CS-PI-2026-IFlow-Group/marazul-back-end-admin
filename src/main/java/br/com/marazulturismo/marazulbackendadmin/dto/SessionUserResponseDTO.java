package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;

public record SessionUserResponseDTO(
        Long id,
        String nome,
        String email
) {
    public static SessionUserResponseDTO fromEntity(Collaborator user) {
        return new SessionUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
