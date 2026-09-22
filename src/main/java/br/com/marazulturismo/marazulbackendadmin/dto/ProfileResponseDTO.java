package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;

import java.util.Comparator;
import java.util.List;

public record ProfileResponseDTO(
        Long id,
        String name,
        List<PermissionResponseDTO> permissions
) {
    private static final Comparator<Permission> BY_BASE_ROUTE_THEN_FEATURE =
            Comparator.comparing(Permission::getBaseRoute).thenComparing(Permission::getFeature);

    public static ProfileResponseDTO fromEntity(Profile profile) {
        List<PermissionResponseDTO> permissions = profile.getPermissions().stream()
                .sorted(BY_BASE_ROUTE_THEN_FEATURE)
                .map(PermissionResponseDTO::fromEntity)
                .toList();

        return new ProfileResponseDTO(profile.getId(), profile.getName(), permissions);
    }
}
