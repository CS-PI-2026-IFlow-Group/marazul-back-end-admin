package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusFuncionario;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.User;

public record FuncionarioResponseDTO(
        Long id,
        String name,
        String email,
        String cellphoneNumber,
        Position position,
        String cnhNumber,
        CNHType cnhType,
        StatusFuncionario status
) {
    public static FuncionarioResponseDTO fromEntity(User user) {
        CNH cnh = user.getCnh();
        return new FuncionarioResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCellphoneNumber(),
                user.getPosition(),
                cnh == null ? null : cnh.getNumber(),
                cnh == null ? null : cnh.getType(),
                user.isInativo() ? StatusFuncionario.INATIVO : StatusFuncionario.ATIVO
        );
    }
}
