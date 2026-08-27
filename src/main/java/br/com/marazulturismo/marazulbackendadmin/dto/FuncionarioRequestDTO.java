package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FuncionarioRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        String name,

        @Email(message = "O e-mail deve possuir um formato válido.")
        String email,

        String cellphoneNumber,

        Position position,

        String cnhNumber,

        CNHType cnhType
) {
}
