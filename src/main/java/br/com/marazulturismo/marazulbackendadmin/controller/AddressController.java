package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.AddressResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enderecos")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponseDTO>> list() {
        return ResponseEntity.ok(addressService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }
}
