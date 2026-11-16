package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ServiceOrderResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.ServiceOrderPdfService;
import br.com.marazulturismo.marazulbackendadmin.service.ServiceOrderService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios/ordem-servico")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;
    private final ServiceOrderPdfService serviceOrderPdfService;

    public ServiceOrderController(ServiceOrderService serviceOrderService,
                                  ServiceOrderPdfService serviceOrderPdfService) {
        this.serviceOrderService = serviceOrderService;
        this.serviceOrderPdfService = serviceOrderPdfService;
    }

    @GetMapping("/{viagemId}")
    public ResponseEntity<ServiceOrderResponseDTO> generate(@PathVariable("viagemId") Long tripId) {
        return ResponseEntity.ok(serviceOrderService.generate(tripId));
    }

    @GetMapping("/{viagemId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("viagemId") Long tripId) {
        ServiceOrderResponseDTO order = serviceOrderService.generate(tripId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(serviceOrderPdfService.fileName(order))
                        .build()
                        .toString())
                .body(serviceOrderPdfService.render(order));
    }
}
