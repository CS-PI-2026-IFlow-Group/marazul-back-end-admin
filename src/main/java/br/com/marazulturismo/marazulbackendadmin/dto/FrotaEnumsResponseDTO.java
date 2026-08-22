package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ModeloCarroceria;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.enums.TipoVeiculo;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogo dos campos enumerados da frota. Evita que o front-end chumbe as
 * strings dos ENUMs no código: ele consome esta rota para montar os selects,
 * e qualquer valor novo adicionado aqui aparece na tela sem release do front.
 */
public record FrotaEnumsResponseDTO(
        List<EnumOptionDTO> tipos,
        List<EnumOptionDTO> modelos,
        List<EnumOptionDTO> status
) {
    public static FrotaEnumsResponseDTO montar() {
        return new FrotaEnumsResponseDTO(
                Arrays.stream(TipoVeiculo.values())
                        .map(tipo -> new EnumOptionDTO(tipo.name(), tipo.getDescricao()))
                        .toList(),
                Arrays.stream(ModeloCarroceria.values())
                        .map(modelo -> new EnumOptionDTO(modelo.name(), modelo.getDescricao()))
                        .toList(),
                Arrays.stream(StatusVeiculo.values())
                        .map(status -> new EnumOptionDTO(status.name(), status.getDescricao()))
                        .toList()
        );
    }
}
