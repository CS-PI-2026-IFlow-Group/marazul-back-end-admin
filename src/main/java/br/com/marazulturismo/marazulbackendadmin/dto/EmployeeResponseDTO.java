package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.User;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public record EmployeeResponseDTO(
        Long id,
        String name,
        String email,
        String cellphoneNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date admissionDate,
        Position position,
        UserRole userRole,
        String cnhNumber,
        CNHType cnhType,
        EmployeeStatus status
) {
    public static EmployeeResponseDTO fromEntity(User user) {
        CNH cnh = user.getCnh();
        return new EmployeeResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCellphoneNumber(),
                user.getAdmissionDate(),
                user.getPosition(),
                user.getUserRole(),
                cnh == null ? null : cnh.getNumber(),
                cnh == null ? null : cnh.getType(),
                user.isActive() ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE
        );
    }
}
