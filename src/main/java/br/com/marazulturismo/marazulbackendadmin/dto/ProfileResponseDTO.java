package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Profile;

import java.util.Comparator;
import java.util.List;

public record ProfileResponseDTO(Long id, String nome, List<PermissionResponseDTO> permissions) {

    public static ProfileResponseDTO fromEntity(Profile profile) {
        List<PermissionResponseDTO> permissions = profile.getPermissions().stream()
                .sorted(Comparator.comparing(permission -> permission.getId()))
                .map(PermissionResponseDTO::fromEntity)
                .toList();

        return new ProfileResponseDTO(profile.getId(), profile.getName(), permissions);
    }
}
