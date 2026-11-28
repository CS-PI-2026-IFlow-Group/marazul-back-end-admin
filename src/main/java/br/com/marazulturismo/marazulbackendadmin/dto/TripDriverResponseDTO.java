package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;

public record TripDriverResponseDTO(Long id, String name) {

    public static TripDriverResponseDTO fromEntity(Collaborator collaborator) {
        return new TripDriverResponseDTO(collaborator.getId(), collaborator.getName());
    }
}
