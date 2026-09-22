package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ProfileResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/perfis")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponseDTO>> list() {
        return ResponseEntity.ok(profileService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.findById(id));
    }
}
