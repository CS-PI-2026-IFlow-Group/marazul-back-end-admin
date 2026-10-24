package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.time.LocalDateTime;

public record EmployeeResponseDTO(
        Long id,
        String name,
        String email,
        String cellphoneNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date admissionDate,
        Position position,
        Boolean isUser,
        Long profileId,
        LocalDateTime lastAccess,
        String cnhNumber,
        CNHType cnhType,
        EmployeeStatus status,
        // Só aparece quando o e-mail de definição de senha não pôde ser enviado
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning
) {
    public EmployeeResponseDTO withWarning(String warning) {
        return new EmployeeResponseDTO(id, name, email, cellphoneNumber, admissionDate, position, isUser,
                profileId, lastAccess, cnhNumber, cnhType, status, warning);
    }

    public static EmployeeResponseDTO fromEntity(Collaborator user) {
        CNH cnh = user.getCnh();
        return new EmployeeResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCellphoneNumber(),
                user.getAdmissionDate(),
                user.getPosition(),
                user.getIsUser(),
                user.getProfile().getId(),
                user.getLastAccess(),
                cnh == null ? null : cnh.getNumber(),
                cnh == null ? null : cnh.getType(),
                user.isActive() ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE,
                null
        );
    }
}
