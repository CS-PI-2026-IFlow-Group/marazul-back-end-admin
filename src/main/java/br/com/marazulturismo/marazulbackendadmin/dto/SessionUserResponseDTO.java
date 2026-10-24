package br.com.marazulturismo.marazulbackendadmin.dto;

import java.util.List;

import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;

public record SessionUserResponseDTO(
        Long id,
        String nome,
        String email,
        String perfil,
        List<String> permissoes
) {
    public static SessionUserResponseDTO fromEntity(Collaborator user, String perfil, List<String> permissoes) {
        return new SessionUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                perfil,
                permissoes
        );
    }
}
