package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.TripResultResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.TripResultPdfService;
import br.com.marazulturismo.marazulbackendadmin.service.TripResultService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/resultado-viagens/viagem")
public class TripResultController {

    private final TripResultService tripResultService;
    private final TripResultPdfService tripResultPdfService;

    public TripResultController(TripResultService tripResultService, TripResultPdfService tripResultPdfService) {
        this.tripResultService = tripResultService;
        this.tripResultPdfService = tripResultPdfService;
    }

    @GetMapping("/{viagemId}")
    public ResponseEntity<TripResultResponseDTO> generate(@PathVariable("viagemId") Long tripId) {
        return ResponseEntity.ok(tripResultService.generate(tripId));
    }

    @GetMapping("/{viagemId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("viagemId") Long tripId) {
        TripResultResponseDTO result = tripResultService.generate(tripId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(tripResultPdfService.fileName(result))
                        .build()
                        .toString())
                .body(tripResultPdfService.render(result));
    }
}
