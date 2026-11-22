package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerReceiptResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/recibo")
public class PassengerReceiptController {

    private final PassengerReceiptService passengerReceiptService;

    public PassengerReceiptController(PassengerReceiptService passengerReceiptService) {
        this.passengerReceiptService = passengerReceiptService;
    }

    @GetMapping("/{viagemId}/{passageiroId}")
    public ResponseEntity<PassengerReceiptResponseDTO> generate(
            @PathVariable("viagemId") Long tripId,
            @PathVariable("passageiroId") Long passengerId) {
        return ResponseEntity.ok(passengerReceiptService.generate(tripId, passengerId));
    }
}
