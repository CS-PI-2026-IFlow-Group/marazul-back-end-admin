package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.PassengerResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerService;
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
@RequestMapping("/api/passageiros")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @GetMapping
    public ResponseEntity<List<PassengerResponseDTO>> list(
            @RequestParam(value = "busca", required = false) String busca) {
        return ResponseEntity.ok(passengerService.list(busca));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(passengerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PassengerResponseDTO> create(@RequestBody @Valid PassengerRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passengerService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PassengerResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid PassengerRequestDTO dto) {
        return ResponseEntity.ok(passengerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        passengerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
