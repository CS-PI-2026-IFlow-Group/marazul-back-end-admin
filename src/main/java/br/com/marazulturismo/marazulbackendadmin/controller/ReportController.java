package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportSummaryDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTableResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.ReportPdfService;
import br.com.marazulturismo.marazulbackendadmin.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class ReportController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;

    public ReportController(ReportService reportService, ReportPdfService reportPdfService) {
        this.reportService = reportService;
        this.reportPdfService = reportPdfService;
    }

    @GetMapping
    public ResponseEntity<List<ReportSummaryDTO>> list() {
        return ResponseEntity.ok(reportService.available());
    }

    @GetMapping("/{relatorio}")
    public ResponseEntity<ReportTableResponseDTO> table(
            @PathVariable String relatorio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return ResponseEntity.ok(reportService.generate(relatorio, dataInicio, dataFim));
    }

    @GetMapping("/{relatorio}/pdf")
    public ResponseEntity<byte[]> pdf(
            @PathVariable String relatorio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        ReportTableResponseDTO report = reportService.generate(relatorio, dataInicio, dataFim);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(reportPdfService.fileName(report))
                        .build()
                        .toString())
                .body(reportPdfService.render(report));
    }
}
