package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.FleetEnumsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VehicleRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VehicleResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.service.FleetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/frota")
public class FleetController {

    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> list(
            @RequestParam(required = false) VehicleStatus status) {
        return ResponseEntity.ok(fleetService.list(status));
    }

    @GetMapping("/enums")
    public ResponseEntity<FleetEnumsResponseDTO> listEnums() {
        return ResponseEntity.ok(fleetService.listEnums());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(fleetService.findById(id));
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(@RequestBody @Valid VehicleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fleetService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(@PathVariable Long id,
                                                     @RequestBody @Valid VehicleRequestDTO dto) {
        return ResponseEntity.ok(fleetService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fleetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
