package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

public record EmployeeRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        String name,

        @Email(message = "O e-mail deve possuir um formato válido.")
        String email,

        String cellphoneNumber,

        @NotNull(message = "O cargo é obrigatório.")
        Position position,

        String cnhNumber,

        CNHType cnhType,

        EmployeeStatus status
) {
    public EmployeeRequestDTO {
        if (email != null) {
            email = email.trim();
            email = email.isEmpty() ? null : email.toLowerCase(Locale.ROOT);
        }
    }
}
