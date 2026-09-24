package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.config.DataInitializer;
import br.com.marazulturismo.marazulbackendadmin.dto.ProfileRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ProfileResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ProfileNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ProfileValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PermissionRepository permissionRepository;
    private final CollaboratorRepository collaboratorRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            PermissionRepository permissionRepository,
            CollaboratorRepository collaboratorRepository) {
        this.profileRepository = profileRepository;
        this.permissionRepository = permissionRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponseDTO> list() {
        return profileRepository.findAll().stream()
                .map(ProfileResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponseDTO findById(Long id) {
        return ProfileResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public ProfileResponseDTO create(ProfileRequestDTO dto) {
        validateNameAvailability(dto.nome(), null);

        Profile profile = new Profile(dto.nome());
        profile.update(dto.nome(), findPermissions(dto.permissionsIds()));
        return ProfileResponseDTO.fromEntity(profileRepository.save(profile));
    }

    @Transactional
    public ProfileResponseDTO update(Long id, ProfileRequestDTO dto) {
        Profile profile = findEntity(id);
        validateNameAvailability(dto.nome(), id);

        profile.update(dto.nome(), findPermissions(dto.permissionsIds()));
        return ProfileResponseDTO.fromEntity(profileRepository.save(profile));
    }

    @Transactional
    public void delete(Long id) {
        Profile profile = findEntity(id);

        if (DataInitializer.ADMIN_PROFILE_NAME.equalsIgnoreCase(profile.getName())) {
            throw new ProfileValidationException("O perfil padrao Administrador nao pode ser excluido.");
        }
        if (collaboratorRepository.existsByProfileAndDisabledAtIsNull(profile)) {
            throw new ProfileValidationException(
                    "O perfil nao pode ser excluido porque esta em uso por colaboradores ativos.");
        }

        profileRepository.delete(profile);
    }

    private Profile findEntity(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
    }

    private void validateNameAvailability(String name, Long profileId) {
        boolean exists = profileId == null
                ? profileRepository.existsByNameIgnoreCase(name)
                : profileRepository.existsByNameIgnoreCaseAndIdNot(name, profileId);

        if (exists) {
            throw new ProfileValidationException("Ja existe um perfil cadastrado com este nome.");
        }
    }

    private Set<Permission> findPermissions(Set<Long> permissionIds) {
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));

        if (permissions.size() != permissionIds.size()) {
            throw new ProfileValidationException("Uma ou mais permissoes informadas nao existem.");
        }

        return permissions;
    }
}
