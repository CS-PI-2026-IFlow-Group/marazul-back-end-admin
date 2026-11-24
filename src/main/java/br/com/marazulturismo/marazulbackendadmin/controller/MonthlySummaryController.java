package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.MonthlySummaryResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.MonthlySummaryPdfService;
import br.com.marazulturismo.marazulbackendadmin.service.MonthlySummaryService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/resumo-mensal")
public class MonthlySummaryController {

    private final MonthlySummaryService monthlySummaryService;
    private final MonthlySummaryPdfService monthlySummaryPdfService;

    public MonthlySummaryController(MonthlySummaryService monthlySummaryService,
                                    MonthlySummaryPdfService monthlySummaryPdfService) {
        this.monthlySummaryService = monthlySummaryService;
        this.monthlySummaryPdfService = monthlySummaryPdfService;
    }

    @GetMapping
    public ResponseEntity<MonthlySummaryResponseDTO> summary(
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(monthlySummaryService.generate(mes));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String mes) {
        MonthlySummaryResponseDTO summary = monthlySummaryService.generate(mes);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(monthlySummaryPdfService.fileName(summary))
                        .build()
                        .toString())
                .body(monthlySummaryPdfService.render(summary));
    }
}
