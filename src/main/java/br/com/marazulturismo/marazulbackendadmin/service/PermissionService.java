package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PermissionResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> list() {
        return permissionRepository.findAllByOrderByBaseRouteAscFeatureAsc().stream()
                .map(PermissionResponseDTO::fromEntity)
                .toList();
    }
}
