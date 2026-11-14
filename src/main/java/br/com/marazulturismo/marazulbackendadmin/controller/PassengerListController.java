package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerListPdfService;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerListService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/lista-passageiros")
public class PassengerListController {

    private final PassengerListService passengerListService;
    private final PassengerListPdfService passengerListPdfService;

    public PassengerListController(PassengerListService passengerListService,
                                   PassengerListPdfService passengerListPdfService) {
        this.passengerListService = passengerListService;
        this.passengerListPdfService = passengerListPdfService;
    }

    @GetMapping("/{viagemId}")
    public ResponseEntity<PassengerListResponseDTO> generate(@PathVariable("viagemId") Long tripId) {
        return ResponseEntity.ok(passengerListService.generate(tripId));
    }

    @GetMapping("/{viagemId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("viagemId") Long tripId) {
        PassengerListResponseDTO list = passengerListService.generate(tripId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(passengerListPdfService.fileName(list))
                        .build()
                        .toString())
                .body(passengerListPdfService.render(list));
    }
}
