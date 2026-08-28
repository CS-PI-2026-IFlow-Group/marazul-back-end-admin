package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;

import java.util.Arrays;
import java.util.List;

public record EmployeeEnumsResponseDTO(
        List<EnumOptionDTO> positions,
        List<EnumOptionDTO> cnhTypes,
        List<EnumOptionDTO> statuses
) {
    public static EmployeeEnumsResponseDTO build() {
        return new EmployeeEnumsResponseDTO(
                Arrays.stream(Position.values())
                        .map(position -> new EnumOptionDTO(position.name(), position.getLabel()))
                        .toList(),
                Arrays.stream(CNHType.values())
                        .map(type -> new EnumOptionDTO(type.name(), type.getLabel()))
                        .toList(),
                Arrays.stream(EmployeeStatus.values())
                        .map(status -> new EnumOptionDTO(status.name(), status.getLabel()))
                        .toList()
        );
    }
}
