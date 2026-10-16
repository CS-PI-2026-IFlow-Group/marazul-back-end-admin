package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.City;

public record CityListResponseDTO(Long id, String name, Long stateId, String stateSigla) {
    public static CityListResponseDTO from(City city) {
        return new CityListResponseDTO(city.getId(), city.getName(),
                city.getState().getId(), city.getState().getAcronym());
    }
}
