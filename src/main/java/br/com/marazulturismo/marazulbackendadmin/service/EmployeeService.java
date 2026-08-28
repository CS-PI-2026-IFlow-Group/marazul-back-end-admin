package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeEnumsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class EmployeeService {

    private final UserRepository userRepository;

    public EmployeeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> list(Position position, EmployeeStatus status) {
        return findEmployees(position, status).stream()
                .map(EmployeeResponseDTO::fromEntity)
                .toList();
    }

    public EmployeeEnumsResponseDTO listEnums() {
        return EmployeeEnumsResponseDTO.build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        return EmployeeResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        validateEmailAvailability(dto.email(), null);
        CNH cnh = buildCnh(dto);

        User employee = new User(
                dto.name(),
                new Date(),
                dto.position(),
                UserRole.USER,
                dto.email(),
                dto.cellphoneNumber(),
                cnh,
                null
        );
        applyStatus(employee, dto.status());

        return EmployeeResponseDTO.fromEntity(userRepository.save(employee));
    }

    @Transactional
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto) {
        User employee = findEntity(id);
        validateEmailAvailability(dto.email(), id);
        CNH cnh = buildCnh(dto);

        employee.update(dto.name(), dto.email(), dto.cellphoneNumber(), dto.position(), cnh);
        applyStatus(employee, dto.status());
        return EmployeeResponseDTO.fromEntity(userRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        User employee = findEntity(id);
        employee.deactivate();
        userRepository.save(employee);
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private List<User> findEmployees(Position position, EmployeeStatus status) {
        if (position == null && status == null) {
            return userRepository.findAll();
        }
        if (status == null) {
            return userRepository.findByPosition(position);
        }
        if (position == null) {
            return status == EmployeeStatus.ACTIVE
                    ? userRepository.findByDisabledAtIsNull()
                    : userRepository.findByDisabledAtIsNotNull();
        }
        return status == EmployeeStatus.ACTIVE
                ? userRepository.findByPositionAndDisabledAtIsNull(position)
                : userRepository.findByPositionAndDisabledAtIsNotNull(position);
    }

    private void validateEmailAvailability(String email, Long employeeId) {
        if (email == null) {
            return;
        }

        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(employeeId))
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException(email);
                });
    }

    private static CNH buildCnh(EmployeeRequestDTO dto) {
        if (dto.position() != Position.DRIVER) {
            return null;
        }

        if (dto.cnhNumber() == null || dto.cnhNumber().isBlank() || dto.cnhType() == null) {
            throw new EmployeeValidationException(
                    "CNH e categoria da CNH são obrigatórias para funcionários motoristas.");
        }

        return new CNH(dto.cnhNumber().trim(), dto.cnhType());
    }

    private static void applyStatus(User employee, EmployeeStatus status) {
        if (status == EmployeeStatus.ACTIVE) {
            employee.activate();
        } else if (status == EmployeeStatus.INACTIVE) {
            employee.deactivate();
        }
    }
}
