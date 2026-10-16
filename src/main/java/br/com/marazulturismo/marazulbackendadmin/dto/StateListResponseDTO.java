package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.State;

public record StateListResponseDTO(Long id, String name, String acronym) {
    public static StateListResponseDTO from(State state) {
        return new StateListResponseDTO(state.getId(), state.getName(), state.getAcronym());
    }
}
