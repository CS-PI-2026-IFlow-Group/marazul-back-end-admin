package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.City;

public record CityDetailResponseDTO(Long id, String name, Long stateId,
                                    String stateNome, String stateSigla) {
    public static CityDetailResponseDTO from(City city) {
        return new CityDetailResponseDTO(city.getId(), city.getName(),
                city.getState().getId(), city.getState().getName(), city.getState().getAcronym());
    }
}
