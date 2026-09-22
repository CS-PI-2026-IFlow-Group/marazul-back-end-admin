package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Permission;

public record PermissionResponseDTO(
        Long id,
        String rotaBase,
        String funcionalidade
) {
    public static PermissionResponseDTO fromEntity(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getBaseRoute(),
                permission.getFeature()
        );
    }
}
