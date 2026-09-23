package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.ProfileResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ProfileNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponseDTO> list() {
        return profileRepository.findAll().stream()
                .sorted(Comparator.comparing(Profile::getName))
                .map(ProfileResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponseDTO findById(Long id) {
        return ProfileResponseDTO.fromEntity(findEntity(id));
    }

    private Profile findEntity(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
    }
}
