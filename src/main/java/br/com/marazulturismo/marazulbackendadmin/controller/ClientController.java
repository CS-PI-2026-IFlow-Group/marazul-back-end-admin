package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ClientDetailResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<List<ClientListResponseDTO>> list(
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(clientService.list(busca));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDetailResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClientDetailResponseDTO> create(@RequestBody @Valid ClientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }
}
