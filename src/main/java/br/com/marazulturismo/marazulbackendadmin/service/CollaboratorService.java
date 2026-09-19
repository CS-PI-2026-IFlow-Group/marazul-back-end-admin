package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeEnumsResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.EmployeeResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.EmployeeStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.EmployeeValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class CollaboratorService {

    private final CollaboratorRepository userRepository;
    private final ProfileRepository profileRepository;

    public CollaboratorService(CollaboratorRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
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
        validateEmail(dto.email(), dto.isUser(), null);
        CNH cnh = buildCnh(dto);
        Profile profile = findProfile(dto.profileId());

        Date admissionDate = dto.admissionDate() != null ? dto.admissionDate() : new Date();

        Collaborator employee = new Collaborator(
                dto.name(),
                admissionDate,
                dto.position(),
                dto.isUser(),
                profile,
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
        Collaborator employee = findEntity(id);
        validateEmail(dto.email(), dto.isUser(), id);
        CNH cnh = buildCnh(dto);
        Profile profile = findProfile(dto.profileId());

        employee.update(
                dto.name(),
                dto.email(),
                dto.cellphoneNumber(),
                dto.position(),
                dto.isUser(),
                profile,
                dto.admissionDate(),
                cnh
        );
        applyStatus(employee, dto.status());
        return EmployeeResponseDTO.fromEntity(userRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        Collaborator employee = findEntity(id);
        employee.deactivate();
        userRepository.save(employee);
    }

    private Collaborator findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private List<Collaborator> findEmployees(Position position, EmployeeStatus status) {
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

    private void validateEmail(String email, Boolean isUser, Long employeeId) {
        if (Boolean.TRUE.equals(isUser) && email == null) {
            throw new EmployeeValidationException("O e-mail é obrigatório para colaboradores com acesso ao sistema.");
        }
        if (email == null) {
            return;
        }

        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(employeeId))
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException(email);
                });
    }

    private Profile findProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new EmployeeValidationException("O perfil informado não existe."));
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

    private static void applyStatus(Collaborator employee, EmployeeStatus status) {
        if (status == EmployeeStatus.ACTIVE) {
            employee.activate();
        } else if (status == EmployeeStatus.INACTIVE) {
            employee.deactivate();
        }
    }
}
