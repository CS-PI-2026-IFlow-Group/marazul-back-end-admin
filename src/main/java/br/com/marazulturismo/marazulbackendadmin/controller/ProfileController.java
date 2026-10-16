package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ProfileRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ProfileResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<ProfileResponseDTO> create(@RequestBody @Valid ProfileRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid ProfileRequestDTO dto) {
        return ResponseEntity.ok(profileService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
