package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email,

        String cellphoneNumber,

        String cnhNumber,

        CNHType cnhType,

        Position position,

        @NotNull
        UserRole userRole
) {}
