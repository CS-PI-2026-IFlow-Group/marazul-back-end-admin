package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.CityDetailResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.CityListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.StateListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.StateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;

    public LocationService(StateRepository stateRepository, CityRepository cityRepository) {
        this.stateRepository = stateRepository;
        this.cityRepository = cityRepository;
    }

    public List<StateListResponseDTO> listStates() {
        return stateRepository.findAllByOrderByNameAsc().stream()
                .map(StateListResponseDTO::from).toList();
    }

    public List<CityListResponseDTO> listCities(Long stateId, String name) {
        if (stateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estadoId ou stateId é obrigatório");
        }
        if (!stateRepository.existsById(stateId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estado não encontrado");
        }
        String search = name == null ? "" : name.trim();
        return (search.isEmpty()
                ? cityRepository.findByStateIdOrderByNameAsc(stateId)
                : cityRepository.findByStateIdAndNameContainingIgnoreCaseOrderByNameAsc(stateId, search))
                .stream().map(CityListResponseDTO::from).toList();
    }

    public CityDetailResponseDTO findCity(Long id) {
        return cityRepository.findById(id)
                .map(CityDetailResponseDTO::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade não encontrada"));
    }
}
