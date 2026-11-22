package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerReceiptResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerReceiptService;
import br.com.marazulturismo.marazulbackendadmin.service.PassengerReceiptPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios/recibo")
public class PassengerReceiptController {

    private final PassengerReceiptService passengerReceiptService;
    private final PassengerReceiptPdfService passengerReceiptPdfService;

    public PassengerReceiptController(PassengerReceiptService passengerReceiptService,
                                      PassengerReceiptPdfService passengerReceiptPdfService) {
        this.passengerReceiptService = passengerReceiptService;
        this.passengerReceiptPdfService = passengerReceiptPdfService;
    }

    @GetMapping("/{viagemId}/{passageiroId}")
    public ResponseEntity<PassengerReceiptResponseDTO> generate(
            @PathVariable("viagemId") Long tripId,
            @PathVariable("passageiroId") Long passengerId) {
        return ResponseEntity.ok(passengerReceiptService.generate(tripId, passengerId));
    }

    @GetMapping("/{viagemId}/{passageiroId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("viagemId") Long tripId,
                                      @PathVariable("passageiroId") Long passengerId) {
        PassengerReceiptResponseDTO receipt = passengerReceiptService.generate(tripId, passengerId);
        return pdfResponse(List.of(receipt), "recibo-viagem-" + tripId + "-passageiro-" + passengerId + ".pdf");
    }

    @GetMapping("/{viagemId}/pdf")
    public ResponseEntity<byte[]> batchPdf(@PathVariable("viagemId") Long tripId) {
        return pdfResponse(passengerReceiptService.generateAll(tripId), "recibos-viagem-" + tripId + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(List<PassengerReceiptResponseDTO> receipts, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment().filename(fileName).build().toString())
                .body(passengerReceiptPdfService.render(receipts));
    }
}
