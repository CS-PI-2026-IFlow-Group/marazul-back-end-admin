package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.CityDetailResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.CityListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.StateListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/api/estados")
    public List<StateListResponseDTO> listStates() {
        return locationService.listStates();
    }

    @GetMapping("/api/cidades")
    public List<CityListResponseDTO> listCities(
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long stateId,
            @RequestParam(required = false) String name) {
        if (estadoId != null && stateId != null && !estadoId.equals(stateId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estadoId e stateId não podem divergir");
        }
        return locationService.listCities(estadoId != null ? estadoId : stateId, name);
    }

    @GetMapping("/api/cidades/{id}")
    public CityDetailResponseDTO findCity(@PathVariable Long id) {
        return locationService.findCity(id);
    }
}
