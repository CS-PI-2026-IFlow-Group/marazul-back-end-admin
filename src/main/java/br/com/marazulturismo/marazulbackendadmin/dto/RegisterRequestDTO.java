package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

public record RegisterRequestDTO(

        @NotBlank
        String name,

        @Email
        String email,

        String cellphoneNumber,

        String cnhNumber,

        CNHType cnhType,

        Position position,

        @NotNull
        Boolean isUser,

        @NotNull
        Long profileId
) {
    public RegisterRequestDTO {
        if (email != null) {
            email = email.trim();
            email = email.isEmpty() ? null : email.toLowerCase(Locale.ROOT);
        }
    }
}
