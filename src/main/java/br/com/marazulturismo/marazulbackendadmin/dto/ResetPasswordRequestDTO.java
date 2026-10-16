package br.com.marazulturismo.marazulbackendadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
        String novaSenha
) {}
