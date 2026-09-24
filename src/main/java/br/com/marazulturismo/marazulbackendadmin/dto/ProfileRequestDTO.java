package br.com.marazulturismo.marazulbackendadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ProfileRequestDTO(

        @NotBlank(message = "O nome do perfil e obrigatorio.")
        @JsonAlias("name")
        String nome,

        @NotNull(message = "A lista de permissoes e obrigatoria.")
        Set<@NotNull(message = "O ID da permissao e obrigatorio.") Long> permissionsIds
) {
    public ProfileRequestDTO {
        if (nome != null) {
            nome = nome.trim();
        }
    }
}
