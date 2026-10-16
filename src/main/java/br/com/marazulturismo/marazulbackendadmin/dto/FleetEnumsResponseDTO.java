package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;

import java.util.Arrays;
import java.util.List;

public record FleetEnumsResponseDTO(
        List<EnumOptionDTO> types,
        List<EnumOptionDTO> models,
        List<EnumOptionDTO> statuses
) {
    public static FleetEnumsResponseDTO build() {
        return new FleetEnumsResponseDTO(
                Arrays.stream(VehicleType.values())
                        .map(type -> new EnumOptionDTO(type.name(), type.getLabel()))
                        .toList(),
                Arrays.stream(BodyworkModel.values())
                        .map(model -> new EnumOptionDTO(model.name(), model.getLabel()))
                        .toList(),
                Arrays.stream(VehicleStatus.values())
                        .map(status -> new EnumOptionDTO(status.name(), status.getLabel()))
                        .toList()
        );
    }
}
