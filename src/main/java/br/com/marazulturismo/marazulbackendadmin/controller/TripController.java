package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.TripExpensesRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripStatusRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.EnumOptionDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import br.com.marazulturismo.marazulbackendadmin.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/viagens")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ResponseEntity<List<TripListResponseDTO>> list(
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long cidadeId,
            @RequestParam(required = false) Long veiculoId) {
        return ResponseEntity.ok(tripService.list(status, dataInicio, dataFim, cidadeId, veiculoId));
    }

    @GetMapping("/enums")
    public ResponseEntity<List<EnumOptionDTO>> listEnums() {
        return ResponseEntity.ok(tripService.listStatuses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TripResponseDTO> create(@RequestBody @Valid TripRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponseDTO> update(@PathVariable Long id,
                                                  @RequestBody @Valid TripRequestDTO dto) {
        return ResponseEntity.ok(tripService.update(id, dto));
    }

    @PutMapping("/{id}/gastos")
    public ResponseEntity<TripResponseDTO> updateExpenses(@PathVariable Long id,
                                                          @RequestBody @Valid TripExpensesRequestDTO dto) {
        return ResponseEntity.ok(tripService.updateExpenses(id, dto));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<TripResponseDTO> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.confirm(id));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<TripResponseDTO> changeStatus(@PathVariable Long id, @RequestBody @Valid TripStatusRequestDTO dto) {
        return ResponseEntity.ok(tripService.changeStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
